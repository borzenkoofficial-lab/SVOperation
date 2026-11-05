#!/usr/bin/env python3
from pathlib import Path
import sys
path=Path(sys.argv[1] if len(sys.argv)>1 else "src/main/resources/assets/svoperation/models/obj/sv01.obj")
if not path.is_file(): raise SystemExit(f"OBJ missing: {path}")
positions,uvs,normals,faces,triangles= [],[],[],0,0
objects,materials=set(),set()
for number,raw in enumerate(path.read_text(encoding="utf-8").splitlines(),1):
 line=raw.strip()
 if not line or line.startswith("#"): continue
 p=line.split(); tag=p[0]
 if tag=="v": positions.append(p[1:4])
 elif tag=="vt": uvs.append(p[1:3])
 elif tag=="vn": normals.append(p[1:4])
 elif tag in ("o","g") and len(p)>1: objects.add(p[1])
 elif tag=="usemtl" and len(p)>1: materials.add(p[1])
 elif tag=="f":
  if len(p)<4: raise SystemExit(f"Invalid face at line {number}")
  faces+=1;triangles+=len(p)-3
  for ref in p[1:]:
   parts=ref.split("/")
   for slot,values in ((0,positions),(1,uvs),(2,normals)):
    if slot>=len(parts) or not parts[slot]: continue
    i=int(parts[slot]);resolved=i-1 if i>0 else len(values)+i
    if resolved<0 or resolved>=len(values): raise SystemExit(f"Out-of-range OBJ index at line {number}: {ref}")
if len(positions)<100 or faces<100 or triangles<150 or len(objects)<10: raise SystemExit("OBJ polygon mesh is below minimum complexity")
if not {"metal","polymer","detail"}.issubset(materials): raise SystemExit(f"Missing material groups: {materials}")
if not uvs or not normals: raise SystemExit("OBJ UVs or normals missing")
unique_uvs = {tuple(uv) for uv in uvs}
loader = Path("src/main/java/ru/svoperation/model/ObjModelLoader.java").read_text(encoding="utf-8")
if len(unique_uvs) < 16 and "hasUsableUvLayout" not in loader:
 raise SystemExit("Degenerate UV layout detected but the planar UV fallback is missing")
mtl_path = path.with_suffix(".mtl")
if not mtl_path.is_file(): raise SystemExit(f"MTL missing: {mtl_path}")
mtl = mtl_path.read_text(encoding="utf-8")
for material, texture in (("metal", "sv01_metal.png"), ("polymer", "sv01_polymer.png"), ("detail", "sv01_detail.png")):
 if f"newmtl {material}" not in mtl or f"map_Kd {texture}" not in mtl:
  raise SystemExit(f"Missing diffuse map for material {material}: {texture}")
print(f"OBJ verified: {len(positions)} positions, {len(uvs)} UVs, {len(normals)} normals, {faces} polygons, {triangles} triangles, {len(objects)} objects")
if len(unique_uvs) < 16:
 print(f"Degenerate source UVs detected ({len(unique_uvs)} unique); runtime planar UV generation is enabled")
print("MTL verified: metal, polymer and detail have separate diffuse maps")
