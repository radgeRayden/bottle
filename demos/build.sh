#!/usr/bin/env sh

rm -r dist

scopes -e ./build.sc ".gpu.hello-triangle"
scopes -e ./build.sc ".gpu.buffers"
scopes -e ./build.sc ".gpu.textures"
scopes -e ./build.sc ".plonk.sprites"
scopes -e ./build.sc ".snake"

cp ./gpu/textures/linus.jpg ./dist/bin
cp ./plonk/sprites/_Run.png ./dist/bin
cp ./snake/snake.png ./dist/bin
cd ./dist/bin
zip ../../dist.zip ./*
cd ../..
