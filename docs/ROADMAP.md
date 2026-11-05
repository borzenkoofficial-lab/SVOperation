# SVOperation Roadmap

## Stage 0 — Foundation
- [x] Forge 1.21.1 / Java 21 project scaffold
- [x] GitHub Actions build pipeline
- [x] Initial mod JAR build

## Stage 1 — First playable slice
- [x] Register SV-01 rifle and rifle ammunition
- [x] Add a dedicated creative tab
- [x] Add Russian and English item names
- [x] Implement server-authoritative hitscan firing
- [x] Stop shots at solid blocks
- [x] Consume one ammunition item per shot (except Creative mode)
- [x] Add a firing cooldown
- [ ] Verify in a launched Minecraft client and dedicated server
- [ ] Replace temporary vanilla item textures with original art and 3D model

## Stage 2 — Combat foundations
- [ ] Configurable weapon stats and ammunition types
- [ ] Reloading, magazines, recoil and aiming
- [ ] Additional weapons and attachments
- [ ] Armor and equipment

## Stage 3 — World and scenarios
- [ ] NPC factions and behavior
- [ ] Vehicles and transport
- [ ] Maps, objectives and scenario system
- [ ] Dedicated-server networking and compatibility tests

## Quality gates
- Every feature must compile in CI.
- Gameplay logic that changes the world must be server-authoritative.
- Client-only rendering must remain isolated from dedicated-server code.
- No release is called production-ready until client and dedicated-server smoke tests pass.
