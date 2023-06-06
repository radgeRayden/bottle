switch operating-system
case 'linux
    shared-library "libcimgui.so"
case 'windows
    shared-library "cimgui.dll"
default
    error "Unsupported OS"

inline filter-scope (scope pattern)
    pattern as:= string
    fold (scope = (Scope)) for k v in scope
        let name = (k as Symbol as string)
        let match? start end = ('match? pattern name)
        if match?
            'bind scope (Symbol (rslice name end)) v
        else
            scope

header :=
    include
        """"#include "cimgui.h"
        options "-DCIMGUI_DEFINE_ENUMS_AND_STRUCTS"

let imgui-extern = (filter-scope header.extern "^(ig|Im)")
let imgui-typedef = (filter-scope header.typedef "^Im")
let imgui-const = (filter-scope header.const "^(?=Im)")
run-stage;

wgpu := import ..gpu.wgpu
import sdl
.. imgui-extern imgui-typedef imgui-const
    do
        # bool ImGui_ImplWGPU_Init(WGPUDevice device, int num_frames_in_flight, WGPUTextureFormat rt_format, WGPUTextureFormat depth_format = WGPUTextureFormat_Undefined);
        ImplWGPU_Init := extern 'ImGui_ImplWGPU_Init (function bool wgpu.Device i32 wgpu.TextureFormat wgpu.TextureFormat)
        # void ImGui_ImplWGPU_Shutdown();
        ImplWGPU_Shutdown := extern 'ImGui_ImplWGPU_Shutdown (function void)
        # void ImGui_ImplWGPU_NewFrame();
        ImplWGPU_NewFrame := extern 'ImGui_ImplWGPU_NewFrame (function void)
        # void ImGui_ImplWGPU_RenderDrawData(ImDrawData* draw_data, WGPURenderPassEncoder pass_encoder);
        ImplWGPU_RenderDrawData := extern 'ImGui_ImplWGPU_RenderDrawData (function void (mutable@ imgui-typedef.DrawData) wgpu.RenderPassEncoder)
        # void ImGui_ImplWGPU_InvalidateDeviceObjects();
        ImplWGPU_InvalidateDeviceObjects := extern 'ImGui_ImplWGPU_InvalidateDeviceObjects (function void)
        # bool ImGui_ImplWGPU_CreateDeviceObjects();
        ImplWGPU_CreateDeviceObjects := extern 'ImGui_ImplWGPU_CreateDeviceObjects (function bool)

        # bool ImGui_ImplSDL2_InitForVulkan(SDL_Window* window);
        # I'm pretty sure it doesn't actually care about the backend at all
        ImplSDL2_InitForVulkan := extern 'ImGui_ImplSDL2_InitForVulkan (function bool (mutable@ sdl.Window))
        # void ImGui_ImplSDL2_Shutdown();
        ImplSDL2_Shutdown := extern 'ImGui_ImplSDL2_Shutdown (function void)
        # void ImGui_ImplSDL2_NewFrame();
        ImplSDL2_NewFrame := extern 'ImGui_ImplSDL2_NewFrame (function void)
        # bool ImGui_ImplSDL2_ProcessEvent(const SDL_Event* event);
        ImplSDL2_ProcessEvent := extern 'ImGui_ImplSDL2_ProcessEvent (function bool (@ sdl.Event))

        local-scope;
