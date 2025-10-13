#!/usr/bin/env bash
set -euxo pipefail

SCRIPT_SRC=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd $SCRIPT_SRC

rm -rf ./dist ./bottle-demos bottle-demos.zip
mkdir ./dist
mkdir ./dist/bin
mkdir ./dist/obj

export LDFLAGS="$(scopes -e ./setup-dist.sc)"

DEMO_REGEXP="^[^#].+"
if [ $# -gt 0 ]; then
    DEMOS=$*
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    DEMOS="$(cat demo-list.txt | grep -E $DEMO_REGEXP | tr '\n' ' ' | tr '\r' ' ')"
else
    DEMOS="$(cat demo-list.txt demo-list-linux.txt | grep -E $DEMO_REGEXP | tr '\n' ' ')"
fi

pushd ..
SC_BUILD_COMMAND="scopes -e -m .demos.build"
for DEMO in $DEMOS; do
    SC_BUILD_COMMAND="$SC_BUILD_COMMAND .$DEMO"
done
$SC_BUILD_COMMAND
popd

cp -r ./assets ./dist/bin/
mv ./dist/bin ./bottle-demos

if [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    mv ./bottle-demos/physfs.dll ./bottle-demos/libphysfs.dll
    cp /mingw64/bin/libstdc++-6.dll ./bottle-demos/
    cp /mingw64/bin/libgcc_s_seh-1.dll ./bottle-demos/
    cp /mingw64/bin/zlib1.dll ./bottle-demos/
    cp /mingw64/bin/libwinpthread-1.dll ./bottle-demos/
fi

if [ $# -eq 0 ]; then
    zip -r bottle-demos.zip ./bottle-demos/*
fi
