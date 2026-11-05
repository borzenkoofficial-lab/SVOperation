#!/usr/bin/env python3
from pathlib import Path

path = Path("src/main/resources/assets/svoperation/models/obj/geran.obj")
if not path.is_file():
    raise SystemExit(f"Geran OBJ missing: {path}")

positions, uvs, normals = [], [], []
groups, materials = set(), set()
faces = 0
for line_no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
    parts = raw.strip().split()
    if not parts or parts[0].startswith("#"):
        continue
    tag = parts[0]
    if tag == "v":
        positions.append(parts[1:4])
    elif tag == "vt":
        uvs.append(parts[1:3])
    elif tag == "vn":
        normals.append(parts[1:4])
    elif tag == "g" and len(parts) > 1:
        groups.add(parts[1])
    elif tag == "usemtl" and len(parts) > 1:
        materials.add(parts[1])
    elif tag == "f":
        if len(parts) < 4:
            raise SystemExit(f"Invalid face at line {line_no}")
        faces += 1
        for ref in parts[1:]:
            indexes = ref.split("/")
            for slot, values in ((0, positions), (1, uvs), (2, normals)):
                if slot >= len(indexes) or not indexes[slot]:
                    raise SystemExit(f"Missing OBJ index at line {line_no}: {ref}")
                index = int(indexes[slot])
                resolved = index - 1 if index > 0 else len(values) + index
                if resolved < 0 or resolved >= len(values):
                    raise SystemExit(f"Out-of-range OBJ index at line {line_no}: {ref}")

if len(positions) < 250 or len(uvs) < 250 or len(normals) < 200 or faces < 200:
    raise SystemExit("Geran mesh is below the minimum geometry/UV/normal budget")
if len(groups) < 7:
    raise SystemExit(f"Expected at least 7 named mesh groups, got {sorted(groups)}")
if not {"composite", "dark", "edge"}.issubset(materials):
    raise SystemExit(f"Missing material slots: {sorted(materials)}")

mtl_path = path.with_suffix(".mtl")
if not mtl_path.is_file():
    raise SystemExit(f"Geran MTL missing: {mtl_path}")
mtl = mtl_path.read_text(encoding="utf-8")
for name in ("composite", "dark", "edge"):
    if f"newmtl {name}" not in mtl:
        raise SystemExit(f"Missing material definition: {name}")

print(f"Geran OBJ verified: {len(positions)} vertices, {len(uvs)} UVs, {len(normals)} normals, {faces} faces, {len(groups)} groups, {len(materials)} materials")
