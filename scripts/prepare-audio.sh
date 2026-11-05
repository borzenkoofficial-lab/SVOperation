#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/src/generated/resources/assets/svoperation/sounds/sv01"
TMP="$ROOT/build/audio-source"
mkdir -p "$DEST" "$TMP"

download_and_convert() {
  local source="$1" name="$2"
  local input="$TMP/$name.wav" output="$DEST/$name.ogg"
  curl --fail --location --retry 2 --retry-delay 2 "$source" --output "$input"
  test -s "$input"
  ffmpeg -hide_banner -loglevel error -y -i "$input" -af "loudnorm=I=-18:TP=-1.5:LRA=10" -c:a libvorbis -q:a 5 "$output"
  test -s "$output"
}

generate_original_fallbacks() {
  echo "External SFX source unavailable; generating original procedural fallback sounds."
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=white:sample_rate=44100:duration=0.62" -af "highpass=f=100,lowpass=f=7200,volume=0.85,afade=t=out:st=0.08:d=0.54" -c:a libvorbis -q:a 5 "$DEST/fire_01.ogg"
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=0.48" -af "highpass=f=180,lowpass=f=5200,volume=0.72,afade=t=out:st=0.04:d=0.44" -c:a libvorbis -q:a 5 "$DEST/fire_02.ogg"
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=pink:sample_rate=44100:duration=0.34" -af "highpass=f=650,lowpass=f=8500,volume=0.42,afade=t=out:st=0.03:d=0.31" -c:a libvorbis -q:a 5 "$DEST/reload.ogg"
  ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=white:sample_rate=44100:duration=0.20" -af "highpass=f=900,lowpass=f=9000,volume=0.48,afade=t=out:st=0.02:d=0.18" -c:a libvorbis -q:a 5 "$DEST/bolt.ogg"
  for file in "$DEST/fire_01.ogg" "$DEST/fire_02.ogg" "$DEST/reload.ogg" "$DEST/bolt.ogg"; do
    test -s "$file"
  done
}

if ! download_and_convert "https://sfxmint.com/dl/impact-gunshot-08.wav" "fire_01"; then
  generate_original_fallbacks
  exit 0
fi
download_and_convert "https://sfxmint.com/dl/impact-gunshot-16.wav" "fire_02"
download_and_convert "https://sfxmint.com/dl/mechanical-reload-14.wav" "reload"
download_and_convert "https://sfxmint.com/dl/gun-cocking-03.wav" "bolt"
