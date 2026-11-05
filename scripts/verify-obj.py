#!/usr/bin/env python3
from pathlib import Path
import sys

path = Path(sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/assets/svoperation/models/obj/sv01.obj")
if not path.is_file():
    raise SystemExit(f"OBJ missing: {path}")
positions, uvs, normals, faces, objects, materials = 0, 0, 0, 0, set(), set()
triangles = 0
for number, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
    line = raw.strip()
    if not line or line.startswith("#"):
        continue
    parts = line.split()
    tag = parts[0]
    if tag == "v": positions += 1
    elif tag == "vt": uvs += 1
    elif tag == "vn": normals += 1
    elif tag in ("o", "g") and len(parts) > 1: objects.add(parts[1])
    elif tag == "usemtl" and len(parts) > 1: materials.add(parts[1])
    elif tag == "f":
        if len(parts) < 4: raise SystemExit(f"Invalid face at line {number}")
        faces += 1
        triangles += len(parts) - 3
        for ref in parts[1:]:
            fields = ref.split("/")
            if not fields[0].lstrip("-").isdigit() or (len(fields) > 1 and fields[1] and not fields[1].lstrip("-").isdigit()):
                raise SystemExit(f"Invalid OBJ index at line {number}: {ref}")
if positions < 100 or faces < 100 or triangles < 150 or len(objects) < 10:
    raise SystemExit(f"OBJ mesh is too simple: {positions} vertices, {faces} faces, {triangles} triangles, {len(objects)} objects")
if not {"metal", "polymer", "detail"}.issubset(materials):
    raise SystemExit(f"Missing material groups: {materials}")
print(f"OBJ verified: {positions} positions, {uvs} UVs, {normals} normals, {faces} polygons, {triangles} triangles, {len(objects)} objects, {len(materials)} materials")
