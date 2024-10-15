#!/usr/bin/env bash

set -euxo pipefail

g++ -o libcimgui.so -shared -O2 -fPIC -DIMGUI_IMPL_API='extern "C"' -DIMGUI_IMPL_WEBGPU_BACKEND_WGPU cimgui.cpp imgui/imgui.cpp imgui/imgui_demo.cpp imgui/imgui_draw.cpp imgui/imgui_tables.cpp imgui/imgui_widgets.cpp imgui/backends/imgui_impl_wgpu.cpp imgui/backends/imgui_impl_sdl3.cpp -I./include -I./include/SDL3 -I./imgui/backends -I./imgui/ -L./lib -lSDL3 -Wl,-rpath '-Wl,$ORIGIN' -l:libwgpu_native.so -Wl,--export-dynamic
