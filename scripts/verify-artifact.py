#!/usr/bin/env python3
"""Validate the distributable SVOperation Forge JAR, not just Gradle's exit code."""
from __future__ import annotations

import hashlib
import json
import re
import struct
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

REQUIRED_FILES = {
    "META-INF/mods.toml",
    "META-INF/jarjar/metadata.json",
    "ru/svoperation/SVOperation.class",
    "ru/svoperation/item/SV01RifleItem.class",
    "ru/svoperation/registry/SVItems.class",
    "ru/svoperation/registry/SVSounds.class",
    "ru/svoperation/client/SV01RifleRenderer.class",
    "ru/svoperation/model/SV01RifleModel.class",
    "assets/svoperation/geo/sv01.geo.json",
    "assets/svoperation/animations/sv01.animation.json",
    "assets/svoperation/textures/item/sv01.png",
    "assets/svoperation/models/item/sv01.json",
    "assets/svoperation/sounds.json",
    "assets/svoperation/lang/ru_ru.json",
    "assets/svoperation/lang/en_us.json",
    "assets/svoperation/sounds/sv01/fire_01.ogg",
    "assets/svoperation/sounds/sv01/fire_02.ogg",
    "assets/svoperation/sounds/sv01/reload.ogg",
    "assets/svoperation/sounds/sv01/bolt.ogg",
}


def fail(message: str) -> None:
    raise SystemExit(f"Artifact verification failed: {message}")


def read_json(archive: zipfile.ZipFile, path: str):
    try:
        return json.loads(archive.read(path))
    except Exception as exc:
        fail(f"{path} is missing or invalid JSON: {exc}")


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify-artifact.py <mod.jar>")

    jar_path = Path(sys.argv[1])
    if not jar_path.is_file() or jar_path.stat().st_size < 100_000:
        fail(f"JAR is missing or unexpectedly small: {jar_path}")

    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        missing = sorted(REQUIRED_FILES - names)
        if missing:
            fail("missing required entries: " + ", ".join(missing))

        mods_toml = jar.read("META-INF/mods.toml").decode("utf-8")
        if not re.search(r'modId="svoperation"', mods_toml):
            fail("mods.toml does not declare modId=svoperation")
        if not re.search(r'modId="geckolib"\\s*\\n\\s*mandatory=true', mods_toml):
            fail("mods.toml does not declare mandatory GeckoLib dependency")

        geo = read_json(jar, "assets/svoperation/geo/sv01.geo.json")
        if not geo.get("minecraft:geometry") or not geo["minecraft:geometry"][0].get("bones"):
            fail("GeckoLib geometry has no geometry/bones")
        animations = read_json(jar, "assets/svoperation/animations/sv01.animation.json")
        clips = animations.get("animations", {})
        if not {"animation.sv01.fire", "animation.sv01.reload"}.issubset(clips):
            fail("fire/reload animation clips are missing")
        sounds = read_json(jar, "assets/svoperation/sounds.json")
        for event in ("sv01_fire", "sv01_reload", "sv01_bolt"):
            if event not in sounds:
                fail(f"sound event {event} is missing")

        png = jar.read("assets/svoperation/textures/item/sv01.png")
        if not png.startswith(b"\\x89PNG\\r\\n\\x1a\\n") or len(png) < 24:
            fail("weapon texture is not a valid PNG")
        width, height = struct.unpack(">II", png[16:24])
        if width != 64 or height != 64:
            fail(f"unexpected texture size: {width}x{height}")

        metadata = read_json(jar, "META-INF/jarjar/metadata.json")
        gecko_entry = next(
            (entry for entry in metadata.get("jars", [])
             if entry.get("identifier", {}).get("group") == "software.bernie.geckolib"),
            None,
        )
        if not gecko_entry:
            fail("Jar-in-Jar metadata does not register GeckoLib")
        nested_path = gecko_entry.get("path", "")
        if nested_path not in names:
            fail(f"Jar-in-Jar metadata points to missing file: {nested_path}")

        nested_bytes = jar.read(nested_path)
        with zipfile.ZipFile(__import__("io").BytesIO(nested_bytes)) as nested:
            nested_names = set(nested.namelist())
            if "software/bernie/geckolib/renderer/GeoItemRenderer.class" not in nested_names:
                fail("embedded GeckoLib JAR lacks GeoItemRenderer")
            nested_mods = nested.read("META-INF/mods.toml").decode("utf-8")
            if not re.search(r'modId="geckolib"', nested_mods):
                fail("embedded dependency is not GeckoLib")

        with tempfile.TemporaryDirectory() as tmp:
            for sound_path in sorted(name for name in names if name.startswith("assets/svoperation/sounds/sv01/") and name.endswith(".ogg")):
                extracted = Path(tmp) / Path(sound_path).name
                extracted.write_bytes(jar.read(sound_path))
                if extracted.stat().st_size < 1024:
                    fail(f"audio file is unexpectedly small: {sound_path}")
                probe = subprocess.run(
                    ["ffprobe", "-v", "error", "-select_streams", "a:0",
                     "-show_entries", "stream=codec_name", "-of", "default=nw=1:nk=1",
                     str(extracted)],
                    capture_output=True, text=True,
                )
                if probe.returncode != 0 or "vorbis" not in probe.stdout.lower():
                    fail(f"audio decode/probe failed for {sound_path}: {probe.stderr.strip()}")

    digest = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    checksum_path = Path(str(jar_path) + ".sha256")
    checksum_path.write_text(f"{digest}  {jar_path.name}\\n", encoding="ascii")
    print(f"Verified {jar_path.name}: {jar_path.stat().st_size} bytes")
    print(f"Verified {len(REQUIRED_FILES)} required entries, compiled classes, GeckoLib Jar-in-Jar, JSON, texture and Vorbis audio")
    print(f"SHA-256: {digest}")


if __name__ == "__main__":
    main()
