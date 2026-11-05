#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/src/generated/resources/assets/svoperation/sounds/sv01"
TMP="$ROOT/build/audio-source"
mkdir -p "$DEST" "$TMP"
download_and_convert() {
  local source="$1" name="$2"
  local input="$TMP/$name.wav" output="$DEST/$name.ogg"
  curl --fail --location --retry 3 --retry-delay 2 "$source" --output "$input"
  test -s "$input"
  ffmpeg -hide_banner -loglevel error -y -i "$input" -af "loudnorm=I=-18:TP=-1.5:LRA=10" -c:a libvorbis -q:a 5 "$output"
  test -s "$output"
}
download_and_convert "https://sfxmint.com/dl/impact-gunshot-08.wav" "fire_01"
download_and_convert "https://sfxmint.com/dl/impact-gunshot-16.wav" "fire_02"
download_and_convert "https://sfxmint.com/dl/mechanical-reload-14.wav" "reload"
download_and_convert "https://sfxmint.com/dl/gun-cocking-03.wav" "bolt"
