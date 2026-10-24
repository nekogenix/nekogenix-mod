import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile


spec = importlib.util.spec_from_file_location('release', Path(__file__).with_name('release.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.properties = 'mod_version=1.0.0-prealpha.16+1.21.1\nneo_version=21.1.259'
        self.release = {
            'id': 123, 'tag_name': 'v1.0.0-prealpha.16', 'name': 'Nekogenix 1.0.0-prealpha.16',
            'draft': False, 'published_at': '2026-10-23T22:03:53Z', 'prerelease': True,
            'body': '## Fixed Bugs\n\n- A manually reviewed fix.\n',
            'assets': [{'name': 'nekogenix-neoforge-1.0.0-prealpha.16+1.21.1.jar',
                        'state': 'uploaded', 'digest': 'sha256:' + 'a' * 64}],
        }

    def test_preserves_manual_metadata(self):
        metadata, notes = release.release_metadata(self.release, self.properties, 'alpha')
        self.assertEqual(metadata['filename'], self.release['assets'][0]['name'])
        self.assertEqual(metadata['version'], '1.0.0-prealpha.16+1.21.1')
        self.assertEqual(notes, self.release['body'])

    def test_rejects_unpublished_or_inconsistent_releases(self):
        for field, value in [('draft', True), ('published_at', None), ('body', ''),
                             ('body', 'Automatically generated notes'), ('tag_name', 'v1.0.0-prealpha.17'),
                             ('tag_name', '../../main'), ('name', 'Different release')]:
            with self.subTest(field=field, value=value):
                changed = copy.deepcopy(self.release)
                changed[field] = value
                with self.assertRaises(ValueError):
                    release.release_metadata(changed, self.properties, 'alpha')

    def test_requires_one_checksummed_artifact(self):
        for assets in [[], self.release['assets'] * 2, [dict(self.release['assets'][0], digest=None)]]:
            with self.subTest(assets=assets), self.assertRaises(ValueError):
                release.release_metadata(dict(self.release, assets=assets), self.properties, 'alpha')

    def test_prealpha_cannot_be_accidentally_stable(self):
        for kind in ('release', 'beta', 'invalid'):
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                release.release_metadata(self.release, self.properties, kind)

    def test_artifact_and_changelog_integrity(self):
        metadata, notes = release.release_metadata(self.release, self.properties, 'alpha')
        with tempfile.TemporaryDirectory(prefix='nekogenix-publish-test-') as temporary:
            folder = Path(temporary)
            artifact = folder / metadata['filename']
            with zipfile.ZipFile(artifact, 'w') as jar:
                jar.writestr('META-INF/neoforge.mods.toml',
                             '[[mods]]\nmodId="nekogenix"\nversion="1.0.0-prealpha.16+1.21.1"\n')
            metadata['sha256'] = release.digest(artifact.read_bytes())
            (folder / 'release.json').write_text(json.dumps(metadata), encoding='utf-8')
            (folder / 'changelog.md').write_bytes(notes.encode())
            self.assertEqual(release.validate_bundle(folder), metadata)
            (folder / 'changelog.md').write_text('Changed notes', encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'changelog SHA256'):
                release.validate_bundle(folder)
            (folder / 'changelog.md').write_bytes(notes.encode())
            artifact.write_bytes(b'Changed artifact')
            with self.assertRaisesRegex(ValueError, 'artifact SHA256'):
                release.validate_bundle(folder)

    def test_new_and_completed_publications(self):
        payload = {'sha256': 'a' * 64}
        self.assertEqual(release.ledger_state([], payload, lambda _: []), 'new')
        self.assertEqual(release.ledger_state([{'id': 1, 'payload': payload}], payload,
                                             lambda _: [{'state': 'success'}]), 'published')

    def test_uncertain_or_changed_uploads_never_repeat(self):
        payload = {'sha256': 'a' * 64}
        for statuses in ([], [{'state': 'in_progress'}], [{'state': 'error'}], [{'state': 'failure'}]):
            with self.subTest(statuses=statuses), self.assertRaises(ValueError):
                release.ledger_state([{'id': 1, 'payload': payload}], payload, lambda _: statuses)
        with self.assertRaises(ValueError):
            release.ledger_state([{'id': 1, 'payload': {'sha256': 'changed'}}], payload,
                                 lambda _: [{'state': 'success'}])


if __name__ == '__main__':
    unittest.main()
