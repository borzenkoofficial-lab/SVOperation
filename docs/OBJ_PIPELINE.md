# OBJ weapon asset pipeline

SVOperation uses original Wavefront OBJ meshes as the source format for detailed weapon geometry.

## Supported mesh data

- `v`: positions
- `vt`: texture coordinates
- `vn`: vertex normals
- `o` / `g`: named mesh sections
- `usemtl`: material section labels
- `f`: triangles and polygons (fan triangulation)
- positive and negative OBJ indices
- missing UVs and normals receive safe defaults / generated face normals

MTL material parsing and GPU rendering are separate client-side layers. The first SV-01 mesh is an original in-repository polygonal asset; it is not extracted from GCR.

## Coordinate convention

The weapon longitudinal axis is X. Mesh dimensions are authored in item-space and normalized by the renderer. Moving components should be exported as separate OBJ objects so their pivots can be animated independently.

## Current migration boundary

The OBJ parser and asset validation are the first migration slice. The existing GeckoLib renderer remains active until the dedicated Forge client renderer, resource reload handling, and in-game first/third-person checks are complete. Do not remove the old renderer before those checks pass.
