# SVOperation

A Minecraft Forge mod project for Minecraft Java Edition 1.21.1.

## Current build

The first playable development slice adds:
- SV-01 rifle item;
- rifle ammunition item;
- a dedicated SVOperation creative tab;
- Russian and English item names;
- server-authoritative hitscan firing (48-block maximum range);
- solid-block obstruction;
- one ammunition item consumed per shot outside Creative mode;
- a 10-tick firing cooldown.

The item icons currently reuse vanilla Minecraft textures as temporary placeholders. Original weapon art, 3D models, reload mechanics, additional weapons, vehicles, NPC factions and scenarios are not implemented yet.

## Requirements

- Minecraft Java Edition 1.21.1
- Minecraft Forge 52.x
- Java 21

## Build

Use Gradle 8.8 and Java 21:

```bash
gradle --no-daemon clean build
```

The mod JAR is generated in `build/libs/`.

## Install

Copy the generated `.jar` into `.minecraft/mods` and launch the Forge 1.21.1 profile.

## Development status

This is an early development build. CI compilation does not replace in-game client and dedicated-server testing. See [docs/ROADMAP.md](docs/ROADMAP.md).
