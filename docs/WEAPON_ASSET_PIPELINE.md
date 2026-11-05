# SVOperation weapon asset and animation pipeline

## Runtime target (locked)

- Minecraft Java 1.21.1
- Minecraft Forge 52.1.20
- Java 21
- GeckoLib Forge 4.8.3
- Do not migrate to NeoForge or another Minecraft version as part of this work.

## Current implementation audit

The current SV-01 has two model representations:

1. models/obj/sv01.obj is loaded and drawn by SV01RifleRenderer, a custom BlockEntityWithoutLevelRenderer.
2. geo/sv01.geo.json and animations/sv01.animation.json are GeckoLib assets. SV01RifleItem implements GeoItem and registers triggerable controllers, but its custom renderer is not a GeoItemRenderer. The OBJ draw path does not evaluate GeckoLib bones/controllers.

This split is the first architectural issue to remove. It allows the item class to trigger GeckoLib animations while the visible OBJ is moved by a separate hand-written timer system. Those are not one animation rig.

The current OBJ source has very low geometric density and a degenerate UV layout. The existing texture/UV branch adds a runtime planar UV fallback and three generated diffuse maps, but does not turn the mesh into a high-detail authored asset or provide physically based shading.

## Chosen asset authoring stack

### Authoring

- Blender: primary source of truth for hard-surface mesh construction, bevels, weighted normals, UV unwrap, texture baking, pivots, and source-file preservation. Keep editable .blend files in the asset source area.
- Blockbench with GeckoLib model format/plugin: use for validating/exporting the bone hierarchy and GeckoLib-compatible geometry/animation JSON. It is an interchange/export stage, not a substitute for proper mesh authoring.
- Texture authoring: Substance 3D Painter when available; otherwise use a documented Blender texture-bake workflow or a permissively licensed PBR painting alternative. Keep source textures and export presets.
- Do not import extracted game assets or third-party firearm models/textures without an explicit compatible license. The reference screenshot is a quality target only.

### Runtime representation

Use GeckoLib's native geometry + animation pipeline for the articulated weapon. Keep a single rig and named bones; do not maintain separate OBJ animation transforms.

Proposed bone hierarchy (names are stable API identifiers):

    root
    ├── receiver
    │   ├── charging_handle
    │   ├── bolt
    │   ├── dust_cover
    │   ├── trigger
    │   └── fire_selector
    ├── barrel
    │   ├── muzzle_device
    │   └── front_sight
    ├── handguard
    │   ├── rail_top
    │   ├── rail_left
    │   └── rail_right
    ├── optic_mount
    │   └── optic
    ├── magazine_well
    └── magazine

Grip sockets/markers must be authored and documented in weapon-local coordinates:
- grip_primary: right/main hand palm target on pistol grip.
- grip_support: left/offhand palm target on foregrip.
- grip_magazine: magazine interaction target.
- sight_ads: eye/sight alignment target.
- muzzle: muzzle/flash and tracer origin.

The exact socket coordinates must be calibrated from the final authored model in-game, not guessed from the current OBJ.

## Materials and textures

Author a coherent PBR texture set even though the vanilla Forge item render path does not consume all PBR channels:

- Base Color / Albedo: required runtime texture.
- Normal: preserve for future shader-compatible rendering or bake selected detail into geometry.
- Roughness and Metallic: preserve as source maps; vanilla entity/item render types do not automatically interpret them as PBR.
- Ambient Occlusion: bake carefully, avoid double-darkening in Minecraft lighting.
- Emissive: separate map/material only for optics or indicator elements that need it.

Recommended starting resolution: 2048 px for the primary weapon atlas, with 1K maps for small accessories where appropriate. Use packed atlases/material groups to limit draw calls. Avoid generating procedural noise and calling it a finished material; the final maps need authored wear, edge response, machining, polymer grain, seams, labels and controlled roughness variation.

## Animation architecture

The item owns gameplay state; the client renderer consumes a presentation state. The server remains authoritative for ammo, reload completion, firing and hit results.

Required animation states:
- idle / relaxed carry
- equip and unequip
- aim / ADS
- fire / recoil
- reload: magazine release, magazine removal, fresh magazine insertion, bolt/charging-handle action
- sprint
- inspect
- empty / dry fire

Use GeckoLib animation controllers and named clips on the same rig that is rendered. Use keyframe event markers for sound/particle timing where appropriate. Do not use independent System.nanoTime() timers in the renderer as the authoritative animation clock.

## First-person and third-person hands

Third-person player posing and first-person arm rendering are separate problems.

- Third person: retain a player render pose integration, but replace the generic crossbow pose with weapon-specific arm posing after the model is calibrated.
- First person: use Forge's client-side arm rendering hook to adjust/replace each rendered arm. Keep the vanilla skin/arm mesh initially, and apply calibrated per-arm grip transforms from the same weapon presentation state.
- The primary hand is selected from the player's main arm and the hand actually holding the rifle. The support hand is the opposite arm. Offhand weapon holding must also be handled.
- The support hand must visually meet the grip_support socket; the primary hand must meet grip_primary. A pose that merely moves the rifle or rotates one forearm is not accepted as a finished grip.
- For a true two-handed grip, both arm transforms must be evaluated from the same weapon state and partial tick. During reload/inspect, animate the support hand away from the foregrip and toward grip_magazine/controls, then return it.
- First-person screenshots are a mandatory acceptance test. A successful Java compile does not prove contact alignment.

## Proposed Java separation

    ru.svoperation.weapon
      WeaponDefinition             immutable weapon tuning and socket metadata
      WeaponState                  server-owned ammo/reload/fire state
      WeaponAction                 action enum: IDLE, AIM, FIRE, RELOAD, SPRINT, INSPECT
      WeaponNetwork                validated server/client state messages

    ru.svoperation.client.weapon
      WeaponPresentationState      interpolated client animation state
      WeaponAnimationController    maps actions to GeckoLib clips
      WeaponGripPose               computes primary/support arm transforms
      WeaponRenderEvents           first- and third-person Forge render hooks
      SV01GeoRenderer              GeckoLib GeoItemRenderer for the SV-01 rig

    assets/svoperation
      geo/sv01.geo.json
      animations/sv01.animation.json
      textures/item/sv01_basecolor.png
      textures/item/source/        authored source maps / exported maps
      models/item/sv01.json

This is a target separation, not a claim that all classes already exist. Introduce it incrementally and preserve current server-authoritative hit detection and ammunition rules.

## Migration sequence

1. Fix and verify the existing UV/material branch before building on it.
2. Author a single SV-01 rigged source asset in Blender; validate pivots, normals, UVs, silhouette and attachment sockets.
3. Export geometry/animations to GeckoLib format and make GeoItemRenderer the sole runtime renderer.
4. Port bolt, magazine, fire and reload motion to rig bones and GeckoLib clips; remove duplicate OBJ part timers only after visual parity is demonstrated.
5. Add first-person arm grip transforms via Forge arm rendering hooks, driven by the same action/presentation state.
6. Add aim/sprint/inspect/equip transitions and event-timed audio.
7. Verify client, dedicated server, multiplayer synchronization, resource reload, and first-/third-person views.

## Acceptance gates

- Model: front, side and three-quarter views; no primitive placeholder silhouette; bevels/normals clean; no floating or intersecting parts.
- UV/materials: UV preview has intentional islands; no stretched full-atlas-per-face artifacts; all texture paths resolve; material response readable in Minecraft lighting.
- Rig: stable named bones and pivots; no detached parts; socket positions documented.
- Hands: right and left palms meet their grip targets; no wrist/finger/forearm clipping in idle, ADS, recoil, sprint and reload.
- Animation: every state has an intentional transition; no abrupt snap, desynchronization or magazine/bolt mismatch.
- Build: clean Gradle build and artifact resource verification for the exact commit.
- Runtime: Forge 1.21.1 client launch and screenshot evidence. Dedicated-server startup and multiplayer test for gameplay/network changes.

## Tooling boundary

The repository can contain source assets, scripts, validation and runtime code. Installing Blender or Substance Painter onto the user's computer is not possible from a GitHub source edit. If a connected desktop/3D tool is not exposed in the current session, Blender authoring scripts can be prepared but must be run and visually checked in Blender before being treated as validated assets.
