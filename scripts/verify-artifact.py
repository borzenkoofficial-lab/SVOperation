#!/usr/bin/env python3
from __future__ import annotations
import hashlib, io, json, re, struct, subprocess, sys, tempfile, zipfile
from pathlib import Path

REQUIRED = {
"META-INF/mods.toml","META-INF/jarjar/metadata.json",
"ru/svoperation/SVOperation.class","ru/svoperation/item/SV01RifleItem.class",
"ru/svoperation/registry/SVItems.class","ru/svoperation/registry/SVSounds.class",
"ru/svoperation/client/SV01RifleRenderer.class","ru/svoperation/client/SVClientEvents.class",
"ru/svoperation/model/ObjModel.class","ru/svoperation/model/ObjModelLoader.class",
"ru/svoperation/model/SV01RifleModel.class",
"assets/svoperation/geo/sv01.geo.json","assets/svoperation/animations/sv01.animation.json",
"assets/svoperation/models/obj/sv01.obj","assets/svoperation/models/obj/sv01.mtl",
"assets/svoperation/textures/item/sv01.png","assets/svoperation/models/item/sv01.json",
"assets/svoperation/sounds.json","assets/svoperation/lang/ru_ru.json","assets/svoperation/lang/en_us.json",
"assets/svoperation/sounds/sv01/fire_01.ogg","assets/svoperation/sounds/sv01/fire_02.ogg",
"assets/svoperation/sounds/sv01/reload.ogg","assets/svoperation/sounds/sv01/bolt.ogg",
}

def fail(message):
    raise SystemExit("Artifact verification failed: " + message)

def read_json(archive, path):
    try:
        return json.loads(archive.read(path))
    except Exception as exc:
        fail(f"{path} is missing or invalid JSON: {exc}")

def main():
    if len(sys.argv) != 2:
        fail("usage: verify-artifact.py <mod.jar>")
    path = Path(sys.argv[1])
    if not path.is_file() or path.stat().st_size < 100_000:
        fail(f"JAR is missing or unexpectedly small: {path}")
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        missing = sorted(REQUIRED - names)
        if missing:
            fail("missing entries: " + ", ".join(missing))
        mods = jar.read("META-INF/mods.toml").decode()
        if not re.search(r'modId="svoperation"', mods):
            fail("mods.toml does not declare svoperation")
        if not re.search(r'modId="geckolib"\s*\n\s*mandatory=true', mods):
            fail("mods.toml does not declare mandatory GeckoLib dependency")
        obj = jar.read("assets/svoperation/models/obj/sv01.obj").decode("utf-8")
        if sum(1 for line in obj.splitlines() if line.startswith("f ")) < 100:
            fail("packaged OBJ weapon mesh has insufficient polygon geometry")
        if "usemtl metal" not in obj or "usemtl polymer" not in obj or "usemtl detail" not in obj:
            fail("packaged OBJ material groups are missing")
        mtl = jar.read("assets/svoperation/models/obj/sv01.mtl").decode("utf-8")
        for material in ("metal", "polymer", "detail"):
            if f"newmtl {material}" not in mtl:
                fail(f"MTL material is missing: {material}")
        if len(re.findall(r"^Kd\s+", mtl, re.MULTILINE)) < 3:
            fail("MTL diffuse color definitions are incomplete")
        geo = read_json(jar, "assets/svoperation/geo/sv01.geo.json")
        geometry = geo.get("minecraft:geometry", [])
        if not geometry or not geometry[0].get("bones"):
            fail("GeckoLib geometry has no bones")
        cube_count = sum(len(bone.get("cubes", [])) for bone in geometry[0]["bones"])
        if cube_count < 30:
            fail(f"weapon geometry is too simple: {cube_count} cubes (minimum 30)")
        animations = read_json(jar, "assets/svoperation/animations/sv01.animation.json").get("animations", {})
        if not {"animation.sv01.aim", "animation.sv01.fire", "animation.sv01.reload"}.issubset(animations):
            fail("aim/fire/reload animation clips are missing")
        item_model = read_json(jar, "assets/svoperation/models/item/sv01.json")
        display = item_model.get("display", {})
        if not {"firstperson_righthand", "thirdperson_righthand"}.issubset(display):
            fail("first/third-person item transforms are missing")
        if display["firstperson_righthand"].get("rotation") != [0, 0, 0]:
            fail("first-person rifle axis is not aligned with the view")
        sounds = read_json(jar, "assets/svoperation/sounds.json")
        if not {"sv01_fire", "sv01_reload", "sv01_bolt"}.issubset(sounds):
            fail("one or more sound events are missing")
        png = jar.read("assets/svoperation/textures/item/sv01.png")
        if not png.startswith(b"\x89PNG\r\n\x1a\n") or len(png) < 24:
            fail("weapon texture is not a valid PNG")
        if struct.unpack(">II", png[16:24]) != (512, 512):
            fail("weapon texture must be 512x512")
        metadata = read_json(jar, "META-INF/jarjar/metadata.json")
        gecko = next((x for x in metadata.get("jars", [])
                      if x.get("identifier", {}).get("group") == "software.bernie.geckolib"), None)
        if not gecko or gecko.get("version", {}).get("artifactVersion") != "4.8.3":
            fail("Jar-in-Jar metadata does not pin GeckoLib 4.8.3")
        nested_path = gecko.get("path", "")
        if nested_path not in names:
            fail(f"missing embedded dependency: {nested_path}")
        with zipfile.ZipFile(io.BytesIO(jar.read(nested_path))) as nested:
            if "software/bernie/geckolib/renderer/GeoItemRenderer.class" not in nested.namelist():
                fail("embedded GeckoLib lacks GeoItemRenderer")
            if not re.search(r'modId="geckolib"', nested.read("META-INF/mods.toml").decode()):
                fail("embedded dependency is not GeckoLib")
        with tempfile.TemporaryDirectory() as tmp:
            for sound in sorted(x for x in names if x.startswith("assets/svoperation/sounds/sv01/") and x.endswith(".ogg")):
                audio = Path(tmp) / Path(sound).name
                audio.write_bytes(jar.read(sound))
                if audio.stat().st_size < 1024:
                    fail(f"audio file is unexpectedly small: {sound}")
                result = subprocess.run(
                    ["ffprobe","-v","error","-select_streams","a:0","-show_entries",
                     "stream=codec_name","-of","default=nw=1:nk=1",str(audio)],
                    capture_output=True, text=True)
                if result.returncode or "vorbis" not in result.stdout.lower():
                    fail(f"Vorbis audio validation failed: {sound}: {result.stderr.strip()}")
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    Path(str(path) + ".sha256").write_text(f"{digest}  {path.name}\n", encoding="ascii")
    print(f"Verified {path.name}: {path.stat().st_size} bytes")
    print(f"Verified {len(REQUIRED)} required entries, detailed geometry, transforms, classes, Jar-in-Jar, JSON, texture and Vorbis audio")
    print("SHA-256: " + digest)

if __name__ == "__main__":
    main()
