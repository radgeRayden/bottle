#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_SRC=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_SRC
[ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ] && IS_MINGW_PLATFORM=1 || IS_MINGW_PLATFORM=0

rm -rf ./dist ./bottle-demos bottle-demos.zip
mkdir ./dist
mkdir ./dist/bin
mkdir ./dist/obj

DEMO_REGEXP="^[^#].+"
if [ $# -gt 0 ]; then
    DEMOS=$*
elif [ $IS_MINGW_PLATFORM == 1 ]; then
    DEMOS="$(cat demo-list.txt | grep -E $DEMO_REGEXP | tr '\n' ' ' | tr '\r' ' ')"
else
    DEMOS="$(cat demo-list.txt demo-list-linux.txt | grep -E $DEMO_REGEXP | tr '\n' ' ')"
fi

pushd ..
scopes -e -m .demos.build $DEMOS
popd

cp -r ./assets ./dist/bin/
mv ./dist/bin ./bottle-demos

if [ "$IS_MINGW_PLATFORM" == 1 ]; then
    mv ./bottle-demos/physfs.dll ./bottle-demos/libphysfs.dll
    cp /mingw64/bin/libstdc++-6.dll ./bottle-demos/
    cp /mingw64/bin/libgcc_s_seh-1.dll ./bottle-demos/
    cp /mingw64/bin/zlib1.dll ./bottle-demos/
    cp /mingw64/bin/libwinpthread-1.dll ./bottle-demos/
fi

if [ $# -eq 0 ]; then
    zip -r bottle-demos.zip ./bottle-demos/*
fi
