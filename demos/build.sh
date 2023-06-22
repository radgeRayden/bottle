#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_SRC=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_SRC

rm -rf ./dist ./bottle-demos
mkdir ./dist
mkdir ./dist/bin
mkdir ./dist/obj

export LDFLAGS="$(scopes -e ./setup-dist.sc)"
scopes -e ./build.sc ".gpu.hello-triangle"
scopes -e ./build.sc ".gpu.buffers"
scopes -e ./build.sc ".gpu.textures"
scopes -e ./build.sc ".plonk.sprites"
scopes -e ./build.sc ".snake"

cp ./gpu/textures/linus.jpg ./dist/bin
cp ./plonk/sprites/_Run.png ./dist/bin
cp ./snake/*.png ./dist/bin/
cp ./snake/*.wav ./dist/bin/
cp ./snake/*.ttf ./dist/bin/
mv ./dist/bin ./bottle-demos

if [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    mv ./bottle-demos/physfs.dll ./bottle-demos/libphysfs.dll
    cp /mingw64/bin/libstdc++-6.dll ./bottle-demos/
    cp /mingw64/bin/libgcc_s_seh-1.dll ./bottle-demos/
    cp /mingw64/bin/zlib1.dll ./bottle-demos/
    cp /mingw64/bin/libwinpthread-1.dll ./bottle-demos/
fi

zip bottle-demos.zip ./bottle-demos/*
rm -r dist bottle-demos
