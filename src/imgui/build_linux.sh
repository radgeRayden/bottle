#!/usr/bin/env bash

pushd "./lib"
for lib in *.so; do
    SONAME=$(readelf -d $lib | rg -e ".+Library soname: \[" -r '$1' | rev | cut --complement -c -1 | rev)
    mv $lib $SONAME
done
popd

g++ -o libcimgui.so -shared -O2 -fPIC -DIMGUI_IMPL_API='extern "C"' cimgui.cpp imgui/imgui.cpp imgui/imgui_demo.cpp imgui/imgui_draw.cpp imgui/imgui_tables.cpp imgui/imgui_widgets.cpp imgui/backends/imgui_impl_wgpu.cpp imgui/backends/imgui_impl_sdl2.cpp -I./include -I./include/SDL2 -I./imgui/backends -I./imgui/ -L./lib -lSDL2 -Wl,-rpath '-Wl,$ORIGIN' -l:libwgpu_native.so -Wl,--export-dynamic
