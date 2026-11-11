#!/usr/bin/env bash
# Makes rsvg-convert (two pictures in site/build.sh) available on a GitHub runner, for the Build and
# Publish workflows. First from Ubuntu's own servers, as the runner is Ubuntu. When those cannot be
# reached (they were down on 2026-11-11 and took both workflows with them), from a Debian
# container instead: Debian's servers are separate. That route puts a small rsvg-convert on PATH
# for the following steps, which runs the tool in the container on the current folder.
set -uo pipefail
command -v rsvg-convert >/dev/null && exit 0

if sudo apt-get update -qq && sudo apt-get install -y -qq librsvg2-bin; then
  command -v rsvg-convert >/dev/null && exit 0
fi

echo "::warning::Ubuntu's package servers failed; taking rsvg-convert from a Debian container instead"
image=focus-rsvg:local
docker rm -f focus-rsvg >/dev/null 2>&1 || true
# The fonts stand in for what the og.svg asks for (Helvetica / Arial / sans-serif).
docker run --name focus-rsvg debian:stable-slim sh -c \
  'apt-get update -qq && apt-get install -y -qq --no-install-recommends librsvg2-bin fonts-dejavu-core fonts-liberation >/dev/null' \
  || { echo "::error::Debian's package servers failed as well"; exit 1; }
docker commit focus-rsvg "$image" >/dev/null && docker rm focus-rsvg >/dev/null

bin="$RUNNER_TEMP/rsvg-bin"
mkdir -p "$bin"
cat > "$bin/rsvg-convert" <<EOS
#!/bin/sh
exec docker run --rm -u "\$(id -u):\$(id -g)" -e HOME=/tmp -v "\$PWD:\$PWD" -w "\$PWD" $image rsvg-convert "\$@"
EOS
chmod +x "$bin/rsvg-convert"
echo "$bin" >> "$GITHUB_PATH"
"$bin/rsvg-convert" --version
