import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tomllib
import urllib.error
import urllib.parse
import urllib.request
import zipfile


REPOSITORY = 'nekogenix/nekogenix-mod'
PROJECTS = json.loads(Path(__file__).with_name('projects.json').read_text(encoding='utf-8'))
ROOT = Path(__file__).resolve().parents[2]


def api(url, token, data=None, header='Authorization'):
    headers = {'User-Agent': f'{REPOSITORY} release workflow', 'Accept': 'application/json'}
    if token:
        headers[header] = f'Bearer {token}' if 'api.github.com/' in url else token
    body = None if data is None else json.dumps(data).encode()
    if body is not None:
        headers['Content-Type'] = 'application/json'
    try:
        with urllib.request.urlopen(urllib.request.Request(url, body, headers), timeout=60) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f'API request failed with HTTP {error.code}: {urllib.parse.urlsplit(url).hostname}') from None


def github(path, data=None):
    return api(f'https://api.github.com/repos/{REPOSITORY}/{path}', os.environ['GH_TOKEN'], data)


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def output(name, value):
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as stream:
            stream.write(f'{name}={value}\n')


def summary(text):
    print(text)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as stream:
            stream.write(text + '\n\n')


def digest(content):
    return hashlib.sha256(content).hexdigest()


def release_metadata(release, properties, release_type):
    tag = release['tag_name']
    if not re.fullmatch(r'v\d+\.\d+\.\d+(?:-[a-zA-Z0-9.]+)?', tag):
        raise ValueError('Invalid release tag')
    if release.get('draft') or not release.get('published_at'):
        raise ValueError('Select an already published GitHub release')
    if release_type not in ('alpha', 'beta', 'release'):
        raise ValueError('Invalid platform release type')
    version = dict(line.split('=', 1) for line in properties.splitlines()
                   if '=' in line and not line.lstrip().startswith('#'))['mod_version'].strip()
    base, separator, minecraft = version.partition('+')
    if not separator or tag != f'v{base}' or not re.fullmatch(r'\d+\.\d+(?:\.\d+)?', minecraft):
        raise ValueError('Tag and gradle.properties mod_version disagree')
    if ('alpha' in base and release_type != 'alpha') or ('beta' in base and release_type != 'beta'):
        raise ValueError('The platform release type does not match the version')
    if release_type == 'release' and release.get('prerelease'):
        raise ValueError('A GitHub pre-release cannot be uploaded as stable')
    changelog = release.get('body') or ''
    if not changelog.strip() or not re.search(r'^## (Features|Changes|Fixed Bugs)\s*$', changelog, re.M):
        raise ValueError('A manually written changelog in the project format is required')
    name = f'Nekogenix {base}'
    if release.get('name') != name:
        raise ValueError('Unexpected GitHub release title')
    filename = f'nekogenix-neoforge-{version}.jar'
    assets = [asset for asset in release['assets'] if asset['name'] == filename and asset['state'] == 'uploaded']
    if len(assets) != 1 or not re.fullmatch(r'sha256:[0-9a-f]{64}', assets[0].get('digest') or ''):
        raise ValueError('Expected exactly one published JAR with a GitHub SHA256 digest')
    return {
        'tag': tag, 'version': version, 'name': name, 'minecraft': minecraft,
        'release_type': release_type, 'filename': filename, 'release_id': release['id'],
        'sha256': assets[0]['digest'].removeprefix('sha256:'),
        'changelog_sha256': digest(changelog.encode()),
    }, changelog


def validate_bundle(folder):
    release = json.loads((folder / 'release.json').read_text(encoding='utf-8'))
    if Path(release['filename']).name != release['filename']:
        raise ValueError('Invalid artifact filename')
    if digest((folder / release['filename']).read_bytes()) != release['sha256']:
        raise ValueError('Release artifact SHA256 mismatch')
    if digest((folder / 'changelog.md').read_bytes()) != release['changelog_sha256']:
        raise ValueError('Release changelog SHA256 mismatch')
    with zipfile.ZipFile(folder / release['filename']) as jar:
        metadata = tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())
        mods = [mod for mod in metadata['mods'] if mod['modId'] == 'nekogenix']
        if len(mods) != 1 or mods[0]['version'] != release['version']:
            raise ValueError('JAR embedded version does not match the release')
    return release


def prepare():
    tag = os.environ['RELEASE_TAG']
    if not re.fullmatch(r'v\d+\.\d+\.\d+(?:-[a-zA-Z0-9.]+)?', tag):
        raise ValueError('Invalid release tag')
    platform = os.environ['RELEASE_PLATFORM']
    platforms = list(PROJECTS) if platform == 'both' else [platform]
    if not all(item in PROJECTS for item in platforms):
        raise ValueError('Invalid destination')
    release = github(f'releases/tags/{tag}')
    sha = git('rev-parse', f'refs/tags/{tag}^{{commit}}')
    subprocess.run(['git', 'merge-base', '--is-ancestor', sha, 'origin/main'], cwd=ROOT, check=True)
    properties = git('show', f'{sha}:gradle.properties')
    metadata, changelog = release_metadata(release, properties, os.environ['RELEASE_TYPE'])
    metadata['sha'] = sha
    folder = Path(os.environ['RELEASE_BUNDLE'])
    folder.mkdir(parents=True, exist_ok=False)
    subprocess.run(['gh', 'release', 'download', tag, '--repo', REPOSITORY,
                    '--pattern', metadata['filename'], '--dir', str(folder)], check=True)
    (folder / 'release.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
    (folder / 'changelog.md').write_bytes(changelog.encode())
    validate_bundle(folder)
    output('sha', sha)
    output('platforms', json.dumps(platforms))
    summary(f"Validated {metadata['filename']} from {sha}; SHA256 {metadata['sha256']}")


def fingerprint(release, platform):
    return {key: release[key] for key in ('release_id', 'sha', 'sha256', 'changelog_sha256', 'release_type')} | {
        'platform': platform, 'project': PROJECTS[platform], 'version': release['version'],
    }


def ledger_state(deployments, expected, get_status):
    for deployment in deployments:
        if deployment['payload'] != expected:
            raise ValueError('A previous publication used different files or metadata; refusing to overwrite it')
        statuses = get_status(deployment['id'])
        if statuses and statuses[0]['state'] == 'success':
            return 'published'
        raise ValueError('A previous upload has an uncertain outcome. Inspect the platform and deployment before retrying; no duplicate will be uploaded')
    return 'new'


def modrinth_existing(release):
    token = os.environ['MODRINTH_TOKEN']
    versions = api(f"https://api.modrinth.com/v2/project/{PROJECTS['modrinth']}/version", token)
    matches = [version for version in versions if version['version_number'] == release['version']]
    if not matches:
        return False
    if len(matches) != 1:
        raise ValueError('Multiple Modrinth versions already use this version number')
    version = matches[0]
    files = [file for file in version['files'] if file['filename'] == release['filename']]
    if (len(files) != 1 or files[0]['hashes']['sha512'] != hashlib.sha512(
            (Path(os.environ['RELEASE_BUNDLE']) / release['filename']).read_bytes()).hexdigest()
            or version['version_type'] != release['release_type']
            or version['name'] != release['name']
            or version['changelog'] != (Path(os.environ['RELEASE_BUNDLE']) / 'changelog.md').read_text(encoding='utf-8')
            or version['loaders'] != ['neoforge'] or version['game_versions'] != [release['minecraft']]):
        raise ValueError('The existing Modrinth version differs from this release')
    summary(f"Already published: https://modrinth.com/project/{PROJECTS['modrinth']}/version/{version['id']}")
    return True


def publish():
    folder = Path(os.environ['RELEASE_BUNDLE'])
    release = validate_bundle(folder)
    platform = os.environ['PUBLISH_PLATFORM']
    if platform not in PROJECTS:
        raise ValueError('Invalid publishing platform')
    mode = os.environ.get('PUBLISH_DRY_RUN', 'true')
    if mode not in ('true', 'false'):
        raise ValueError('PUBLISH_DRY_RUN must be true or false')
    dry = mode == 'true'
    task = 'publishModrinth' if platform == 'modrinth' else 'publishCurseforge'
    wrapper = ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
    command = [str(wrapper), '--no-daemon', '--project-dir', str(Path(__file__).parent), task]
    environment = dict(os.environ, PUBLISH_DRY_RUN='true')
    subprocess.run(command, cwd=ROOT, env=environment, check=True)
    if dry:
        summary(f"Dry run passed for {platform}: {release['filename']} ({release['release_type']}); nothing uploaded")
        return
    if platform == 'modrinth' and modrinth_existing(release):
        return
    if platform == 'curseforge':
        api('https://minecraft.curseforge.com/api/game/versions', os.environ['CURSEFORGE_TOKEN'], header='X-Api-Token')
    expected = fingerprint(release, platform)
    environment_name = f"publish-{platform}-{release['tag']}"
    deployments = github('deployments?' + urllib.parse.urlencode({'environment': environment_name, 'per_page': 100}))
    if ledger_state(deployments, expected, lambda item: github(f'deployments/{item}/statuses')) == 'published':
        summary(f"Already published {release['version']} to {platform}; upload skipped")
        return
    deployment = github('deployments', {
        'ref': release['sha'], 'environment': environment_name, 'auto_merge': False,
        'required_contexts': [], 'production_environment': False,
        'description': f"Publish {release['version']} to {platform}", 'payload': expected,
    })
    deployment_id = deployment['id']
    log_url = f"https://github.com/{REPOSITORY}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
    github(f'deployments/{deployment_id}/statuses', {'state': 'in_progress', 'log_url': log_url})
    result_path = Path(__file__).parent / 'build' / 'publishMods' / f'{task}.json'
    result_path.unlink(missing_ok=True)
    try:
        subprocess.run(command, cwd=ROOT, env=dict(os.environ, PUBLISH_DRY_RUN='false'), check=True)
        result = json.loads(result_path.read_text(encoding='utf-8'))
        if str(result['projectId']) != PROJECTS[platform]:
            raise ValueError('Publisher returned an unexpected project')
        url = (f"https://modrinth.com/project/{PROJECTS[platform]}/version/{result['id']}" if platform == 'modrinth'
               else f"https://www.curseforge.com/minecraft/mc-mods/nekogenix/files/{result['fileId']}")
        github(f'deployments/{deployment_id}/statuses', {
            'state': 'success', 'environment_url': url, 'log_url': log_url,
            'description': 'Upload accepted by platform; platform moderation may still be pending',
            'auto_inactive': False,
        })
        summary(f"Upload accepted: {url} ({release['release_type']}); file SHA256 {release['sha256']}")
    except Exception:
        github(f'deployments/{deployment_id}/statuses', {
            'state': 'error', 'log_url': log_url,
            'description': 'Check platform outcome before retrying this upload',
        })
        raise


if __name__ == '__main__':
    try:
        {'prepare': prepare, 'publish': publish}[sys.argv[1]]()
    except Exception as error:
        print(f'::error::{error}', file=sys.stderr)
        sys.exit(1)
