<p align="center">
  <a href="https://nekogenix.github.io/">
    <img src="logo.png" alt="Nekogenix" width="440" />
  </a>
</p>

<p align="center">
  <a href="https://nekogenix.github.io/wiki/">
    <img src=".github/readme/buttons/wiki.svg" alt="Wiki" />
  </a>
  <a href="https://nekogenix.github.io/">
    <img src=".github/readme/buttons/website.svg" alt="Website" />
  </a>
</p>

<p align="center">
  <a href="https://neoforged.net/">
    <img src="https://img.shields.io/badge/Minecraft_Java-NeoForge-b977a0?style=flat-square&amp;labelColor=29232f" alt="Minecraft Java Edition, NeoForge" />
  </a>
</p>

<p align="center">
    <a href="https://liberapay.com/Nekogenix/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a>
</p>

<h2><img src=".github/readme/icons/cat.svg" width="28" height="28" alt="" /> Minecraft as a cat</h2>

Nekogenix is a Minecraft Java Edition mod for NeoForge. A Neko Injection starts a gradual transformation from human to neko, then into a cat with its own model and animations. You can stop the transformation if you’d rather stay a neko.

Keep mining, building and exploring with your items, armour and elytra. Your inventory stays with you, and your equipment adapts to your cat form.

<p align="center">
  <img src=".github/readme/cat.gif" alt="A cat player exploring a cave with armour, elytra, a pickaxe and a torch" width="640" />
</p>

<h2><img src=".github/readme/icons/paw-print.svg" width="28" height="28" alt="" /> Feline abilities</h2>

- **Double-jump.** Jump again in mid-air. Sprinting and sneaking also become faster.
- **Feline Vision.** See even in complete darkness.
- **Soft Paws.** Take less damage from falls.
- **Feline Scent.** Crouch to find creatures through walls, with different colours showing how dangerous they are.

<p align="center">
  <img src=".github/readme/abilities.gif" alt="Double-jumping, seeing in the dark, surviving a high fall and tracking creatures by scent" width="640" />
</p>

Creepers keep their distance, stray cats stop running away from you, and staying awake no longer causes phantoms to spawn. Your smaller body also fits through one-block gaps.

In Survival, [nine lives](https://nekogenix.github.io/wiki/transformation/#lives) let you keep your feline form through eight deaths. The ninth returns you to human form.

<h2><img src=".github/readme/icons/flask-conical.svg" width="28" height="28" alt="" /> Laboratory and transformation</h2>

The laboratory adds syringes, saliva samples, nucleocards, a centrifuge and a pneumatic stand. Use them to [prepare a Neko Injection](https://nekogenix.github.io/wiki/laboratory/#pneumatic). Your abilities appear gradually as the transformation progresses.

A stabilizer lets you [stop before the final change](https://nekogenix.github.io/wiki/transformation/#stabilize) and keep your neko form, including the ears and abilities.

<p align="center">
  <img src=".github/readme/laboratory.gif" alt="Processing ingredients in the laboratory and applying the finished Neko Injection" width="640" />
</p>

<h2><img src=".github/readme/icons/palette.svg" width="28" height="28" alt="" /> Cat skins</h2>

Choose a vanilla cat coat or [import your own skin](https://nekogenix.github.io/wiki/cat-skins/#file). The in-game menu lets you preview it on your cat. Your selected skin also changes the colours of your ears in neko form.

<p align="center">
  <img src=".github/readme/skins.gif" alt="Importing a custom cat skin and choosing a coat in the in-game skin menu" width="640" />
</p>

<h2><img src=".github/readme/icons/download.svg" width="28" height="28" alt="" /> Installation</h2>

1. Download a `.jar` from [GitHub Releases](https://github.com/nekogenix/nekogenix-mod/releases) for your Minecraft version.
2. Install NeoForge for the same Minecraft version.
3. Put the mod’s `.jar` in your game instance’s `mods` folder.

For multiplayer, install Nekogenix on the server and on each player’s client. The [installation guide](https://nekogenix.github.io/wiki/getting-started/) covers the official launcher, Prism Launcher, Modrinth App and CurseForge App.

CurseForge and Modrinth releases are coming soon.

<details>
<summary>Building from source</summary>

Clone this repository and use the Java toolchain specified in [build.gradle](https://github.com/nekogenix/nekogenix-mod/blob/main/build.gradle).

On Linux or macOS:

```sh
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The built `.jar` is in `build/libs/`.

</details>

<h2><img src=".github/readme/icons/scale.svg" width="28" height="28" alt="" /> License</h2>

- **Code:** [PolyForm Noncommercial License 1.0.0](https://github.com/nekogenix/nekogenix-mod/blob/main/LICENSE.md#code-license).
- **Assets under `src/main/resources/assets/`:** [All Rights Reserved](https://github.com/nekogenix/nekogenix-mod/blob/main/LICENSE.md#assets-license), Rudator and Wechirok.
