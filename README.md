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
  <strong>Available on</strong>&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/nekogenix">
    <img src="https://img.shields.io/badge/CURSEFORGE-F16436?style=for-the-badge&logo=curseforge&logoColor=white&labelColor=000000" alt="CurseForge" align="absmiddle" />
  </a>
</p>

<p align="center">
  <a href="https://liberapay.com/Nekogenix/donate"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg"></a>
</p>

> [!NOTE]
> <details>
> <summary>AI disclosure</summary>
>
> <p>We're not hiding that AI helped write most of Nekogenix's code. Does that make it low-quality garbage? Hell NO. We're not the kind of people who throw a prompt at some LLM and sit there like a goddamn cuck waiting for it to finish. We decide HOW things work: libs, server vs client logic, data persistence, networking, nf events vs mixins, rendering, hitboxes, and mods compatibility. We read, review, and test the whole code instead of assuming it works just because it compiles. Every asset/icons/our logo was made by hand. We regularly discuss our ideas with experienced modders in the community and work out the best ways to implement them. Sure, it's a simple mod, but we give a damn about making it good. Slop is everywhere, and we understand wanting to hit "Hide AI content." Half-assed projects deserve the criticism. But we're putting real work into Nekogenix, and we'd recommend it even to hardcore AI haters.</p>
>
> </details>

<h2><img src=".github/readme/icons/cat.svg" width="28" height="28" alt="" /> Minecraft as a cat</h2>

Minecraft already has cats. Nekogenix lets you become one.

Make a Neko Injection and start changing from human to neko, then into a cat with its own model and animations. You pick up feline abilities along the way. If the ears are enough for you, there's a way to stop before the final transformation.

And yes, you can still mine, build, wear armour and fly with elytra. We weren't about to take your inventory away just because you grew paws.

<p align="center">
  <img src=".github/readme/cat.gif" alt="A cat player exploring a cave with armour, elytra, a pickaxe and a torch" width="640" />
</p>

<h2><img src=".github/readme/icons/paw-print.svg" width="28" height="28" alt="" /> Feline abilities</h2>

- **Feline Agility:** Sprint and sneak faster, and jump a second time in mid-air.
- **Feline Vision:** See in places where you'd usually be fumbling around in the dark.
- **Soft Paws:** Survive falls that would make a regular player regret their decisions.
- **Feline Scent:** In full cat form, crouch to spot nearby creatures through walls. Different outline colours tell you what you're dealing with.

<p align="center">
  <img src=".github/readme/abilities.gif" alt="Double-jumping, seeing in the dark, surviving a high fall and tracking creatures by scent" width="640" />
</p>

Creepers keep their distance, stray cats are less skittish around you, and staying awake won't summon phantoms on your behalf. In cat form, you can also get through one-block passages without crawling like a human.

In Survival, you've got [nine lives](https://nekogenix.github.io/wiki/transformation/#lives). The first eight deaths leave your feline form intact. The ninth sends you back to being human. Try not to spend them all in one afternoon.

<h2><img src=".github/readme/icons/flask-conical.svg" width="28" height="28" alt="" /> Laboratory and transformation</h2>

Turning into a cat takes some preparation. You'll collect cat saliva, work with nucleocards, and use a centrifuge and pneumatic stand to [make your Neko Injection](https://nekogenix.github.io/wiki/laboratory/#pneumatic).

Is that a ridiculous amount of equipment just to grow cat ears? Perhaps. We built it anyway.

Abilities unlock as the transformation progresses. A separate injection lets you [stop at neko form](https://nekogenix.github.io/wiki/transformation/#stabilize) and keep your ears and abilities without becoming a full cat. That choice is permanent.

<p align="center">
  <img src=".github/readme/laboratory.gif" alt="Processing ingredients in the laboratory and applying the finished Neko Injection" width="640" />
</p>

<h2><img src=".github/readme/icons/palette.svg" width="28" height="28" alt="" /> Cat skins</h2>

Pick one of Minecraft's 11 vanilla cat coats or [import a custom skin](https://nekogenix.github.io/wiki/cat-skins/#file). You can preview it in-game before committing. Your chosen coat also controls the colours of your neko ears.

<p align="center">
  <img src=".github/readme/skins.gif" alt="Importing a custom cat skin and choosing a coat in the in-game skin menu" width="640" />
</p>

<h2><img src=".github/readme/icons/download.svg" width="28" height="28" alt="" /> Installation</h2>

1. Download a `.jar` from [GitHub Releases](https://github.com/nekogenix/nekogenix-mod/releases) for your Minecraft version.
2. Install NeoForge for the same Minecraft version.
3. Put the mod's `.jar` in your game instance's `mods` folder.

For multiplayer, install Nekogenix on the server and on each player's client. The [installation guide](https://nekogenix.github.io/wiki/getting-started/) covers the official launcher, Prism Launcher, Modrinth App and CurseForge App.

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
