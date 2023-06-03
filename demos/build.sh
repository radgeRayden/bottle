#!/usr/bin/env sh

scopes -e ./build.sc ".gpu.hello-triangle"
scopes -e ./build.sc ".gpu.buffers"
scopes -e ./build.sc ".gpu.textures"

pushd "./dist/bin"
for lib in *.so; do
    SONAME=$(readelf -d $lib | rg -e ".+Library soname: \[" -r '$1' | rev | cut --complement -c -1 | rev)
    mv $lib $SONAME
done
popd
