# World Preview Fabric

*World Preview Fabric* is a mod for visualizing Minecraft world seeds before they are generated.

Find us on [GitHub](https://github.com/s-yh-china/world-preview-fabric) only now!

## Origin

The original project is [World Preview](https://github.com/caeruleusDraconis/world-preview).

Due to the original project's long-term inactivity, I've created this hard fork specifically for the Fabric. Should the original project resume maintenance, this fork may be discontinued.

## Dependencies

| Dependency | Type     | Download                                                                                                                                                                         |
|------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Fabric-API | Required | [Modrinth](https://modrinth.com/mod/fabric-api) &#124; [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) &#124; [Github](https://github.com/FabricMC/fabric) |

## Usage

*World Preview Fabric* adds a new `Preview` tab to the Singleplayer menu.

<img alt="biomes" src="img/open.png" width="1904"/>

Upon opening that tab, a random seed is selected and a map of biomes is generated:

<img alt="biomes" src="img/biomes.png" width="1906"/>

By default, the overworld dimension will be generated, structures will not be shown and no heightmap will be generated.
This can be changed in the Settings (top-left button in the `Preview` tab).

When structure sampling is enabled, the visibility of individual types of structures on the preview can be toggled:

<img alt="structures" src="img/structures.png" width="1906"/>

When height sampling is enabled, the preview can be toggled between the biome map and a colorized heightmap:

<img alt="heightmap" src="img/heightmap.png" width="1906"/>

When y-layer intersection sampling is enabled, the preview can also show the blocks on the current y-layer.
Additionally, the y-layer one step below is also shown in a lighter color:

<img alt="heightmap" src="img/y-int.png" width="1906"/>

Since version 1.1.0, there is also experimental support for opening the preview in-game for a single-player worlds:

<img alt="ingame" src="img/ingame.png" width="1906"/>

##### Moving on the preview

Clicking and dragging on the map-part of the preview tab will move along the x and z axis.
This will cause the following load sequence:

- Any biomes that are not yet sampled on the current y-level
- Structures (if enabled)
- Height map (if enabled)
- Adjacent y-layers (if enabled)

Scrolling does **not** zoom into the map (there is a separate setting in the configuration menu for controlling the zoom level), but instead travels along the y-axis.
This allows cave biomes to be seen as well. Please note that non-cave biomes span the entire world height beyond that.

##### Other features

- Works with mods! Tested with:
    - Terralith
    - Biomes O' Plenty
- Persistent seed storage
- Highlighting specific biomes
- Highly configurable and extendable

## Supported version

This table shows the current support status for the Minecraft version.

| Minecraft Version | Status    |
|-------------------|-----------|
| `1.21.4`          | Supported |

## FAQ

**Q:** *Will Minecraft versions before 1.21.4 be supported?*

**A:** Look [original project](https://github.com/caeruleusDraconis/world-preview).

---

**Q:** *Will Multiplayer be supported?*

**A:** Will.

---

**Q:** *Scrolling does not zoom the preview!*

**A:** Scrolling moves the y-level up and down. To change the zoom level, go to `Settings (top left wrench) -> Resolution` and change the visual size of a chunk.

---

**Q:** *The preview is completely white / black for the y-intersections view.*

**A:** This is likely because the starting y-layer for the preview is the build limit. Try scrolling down to a lower y-layer and something should show up.

---

**Q:** *My CPU is at 100%!*

**A:** You can limit the number of used cores in `Settings (top left wrench) -> General`. By default, *World Preview Fabric* tries to compute the biome preview / structures / heightmap as quickly as possible. These calculations require a lot of CPU power.

## Mod incompatibilities

This mod should be compatible with most mods (including those adding new biomes and dimensions).

## Adding support for new biomes and structures

New biomes, structures, and more can be registered via the Minecraft datapack mechanism. See [the World Preview Fabric dataformat docs](/DataFormats.md) for more information.
