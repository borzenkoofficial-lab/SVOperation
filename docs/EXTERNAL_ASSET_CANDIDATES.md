# SVOperation external asset candidates

Status: research shortlist only. No third-party binary asset has been copied into the runtime resources yet.

## Selection rules

- Prefer CC0 1.0 / public-domain assets with an explicit license statement on the source page.
- Preserve the original source URL and license note alongside every imported asset.
- Do not import a model solely because a search result says "free". Confirm the license on the author's page.
- Reject assets with unclear redistribution rights, missing texture provenance, broken downloads, or unusable topology.
- A source model is not production-ready until scale, axis, normals, UVs, pivots, separate moving parts, texture paths and runtime performance are checked.

## Candidate A — high-poly source for quality evaluation

**Stein Games — Classic Weapons Pack**

- Source: https://stein-indie.itch.io/classic-weapons-pack
- Page states: FBX models, 2048 px textures, high-poly/no LODs, CC0 1.0 Universal.
- Candidate use: inspect as a visual/mesh-quality reference and evaluate one model as a possible source for a derivative SV-01-style game asset.
- Risks to check before import: exact included model list/version, FBX material/texture paths, polygon budget, separate moving parts, rig/pivots and whether all included files carry the same license.

## Candidate B — compact interchange test asset

**OpenGameArt — 3D Gun Part 1 (Free Version)**

- Source: https://opengameart.org/content/3d-gun-part-1-free-version
- Page states: GLTF, FBX and Blender formats; CC0.
- Candidate use: test the import/normalization pipeline and verify GLTF/FBX material handling.
- Not selected as the final SV-01 model until geometry density, texture quality and articulation are inspected.

## Candidate C — broad rigged model set and audio reference

**petroulacl/fps-asset-kit**

- Source: https://github.com/petroulacl/fps-asset-kit
- README states that the pack includes 20 GLB/FBX/OBJ weapon models, with FBX described as rigged; the README declares the weapons CC0 via OpenGameArt.
- README also declares CC0 firearm sound effects from The Free Firearm Sound Library.
- Candidate use: compare rig naming, part separation and interchange formats; use sound assets only after the individual source archive and selected files are verified.
- Important: the pack describes the weapon models as stylized/flat-shaded. It is a pipeline donor/reference, not automatically the final visual quality target.

## Existing GCR archive

The user previously supplied `gcr-1.3.2-BETA-1.21.1-neoforge.zip`. Earlier inspection notes recorded GLTF models with separate nodes and approximately 25k–30k vertices for several weapons, plus OGG sound assets. The original archive bytes are not currently present in the active working directory, so no binary can be extracted from those notes alone. Reattach the archive when direct extraction is needed; do not recreate or claim copied assets from metadata.

## Import and runtime acceptance

1. Download the exact candidate archive and retain its license/source note.
2. Inspect model dimensions, object/node names, triangle count, UV sets, material assignments, texture references and animation/skin data.
3. Convert only after inspection; keep the source file unchanged under an asset-source directory.
4. Build one coherent GeckoLib rig with stable pivots for bolt, charging handle, trigger, magazine and selector. Do not assume a GLB/FBX rig maps directly to GeckoLib bones.
5. Export the runtime geometry and textures, then validate the actual packaged JAR.
6. Launch Forge 1.21.1 and capture first-/third-person evidence before marking the asset accepted.

## Current runtime limitation

Vanilla Forge item rendering does not automatically consume a full metallic/roughness/normal PBR set. Keep those maps as authoring sources; export a base-color texture for the runtime path unless a custom shader/material pipeline is implemented.
