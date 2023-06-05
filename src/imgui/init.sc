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

module := .. imgui-extern imgui-typedef
run-stage;

wgpu := import ..gpu.wgpu
import sdl
import ..window

global reset : bool
.. module
    do
        # bool ImGui_ImplWGPU_Init(WGPUDevice device, int num_frames_in_flight, WGPUTextureFormat rt_format);
        ImplWGPU_Init := extern 'ImGui_ImplWGPU_Init (function bool wgpu.Device i32 wgpu.TextureFormat)
        # void ImGui_ImplWGPU_Shutdown();
        ImplWGPU_Shutdown := extern 'ImGui_ImplWGPU_Shutdown (function void)
        # void ImGui_ImplWGPU_NewFrame();
        ImplWGPU_NewFrame := extern 'ImGui_ImplWGPU_NewFrame (function void)
        # void ImGui_ImplWGPU_RenderDrawData(ImDrawData* draw_data, WGPURenderPassEncoder pass_encoder);
        ImplWGPU_RenderDrawData := extern 'ImGui_ImplWGPU_RenderDrawData (function void (mutable@ module.DrawData) wgpu.RenderPassEncoder)
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

        ig := module

        fn init ()
            using import ..gpu.common

            ig.CreateContext null
            assert
                ImplSDL2_InitForVulkan (window.get-handle)
            assert
                ImplWGPU_Init istate.device 3 (get-preferred-surface-format)
            ()

        fn begin-frame ()
            if reset
                ImplWGPU_CreateDeviceObjects;
                reset = false
                return;

            ImplSDL2_NewFrame;
            ImplWGPU_NewFrame;
            ig.NewFrame;

        fn wgpu-reset ()
            ImplWGPU_InvalidateDeviceObjects;
            reset = true

        fn process-event (event)
            result := ImplSDL2_ProcessEvent event # do we even use this result for anything?
            io := (ig.GetIO)

            switch event.type
            pass sdl.SDL_MOUSEMOTION
            pass sdl.SDL_MOUSEBUTTONDOWN
            pass sdl.SDL_MOUSEBUTTONUP
            pass sdl.SDL_MOUSEWHEEL
            do (deref io.WantCaptureMouse)
            pass sdl.SDL_KEYDOWN
            pass sdl.SDL_KEYUP
            do (deref io.WantCaptureKeyboard)
            default false #result

        fn present (render-pass)
            ig.Render;
            ImplWGPU_RenderDrawData (ig.GetDrawData) (view render-pass)

        fn shutdown ()
            ImplSDL2_Shutdown;
            ImplWGPU_Shutdown;
            ig.Shutdown;

        fn end-frame ()
            ig.EndFrame;

        unlet ig
        local-scope;
