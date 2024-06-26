switch operating-system
case 'linux
    shared-library "libcimgui.so"
case 'windows
    shared-library "cimgui.dll"
default
    error "Unsupported OS"

using import Array enum slice struct include

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
let imgui-typedef = (filter-scope header.typedef "^Im(Gui)?")
run-stage;

vvv bind imgui-typedef
fold (scope = imgui-typedef) for k v in imgui-typedef
    local old-symbols : (Array Symbol)
    T := (v as type)
    if (T < CEnum)
        for k v in ('symbols T)
            original-symbol  := k as Symbol
            original-name    := original-symbol as string
            match? start end := 'match? str"^ImGui.+?_" original-name

            if match?
                field := (Symbol (rslice original-name end))
                'set-symbol T field v
                'append old-symbols original-symbol

        for sym in old-symbols
            sc_type_del_symbol T sym

    name := (k as Symbol as string)
    if ('match? str".+_$" name)
        trimmed-name := lslice name ((countof name) - 1)
        'bind scope (Symbol trimmed-name) v
    else
        scope
run-stage;

wgpu := import wgpu
sdl  := import sdl3
.. imgui-extern imgui-typedef
    do
        #   struct ImGui_ImplWGPU_InitInfo
            {
                WGPUDevice              Device;
                int                     NumFramesInFlight = 3;
                WGPUTextureFormat       RenderTargetFormat = WGPUTextureFormat_Undefined;
                WGPUTextureFormat       DepthStencilFormat = WGPUTextureFormat_Undefined;
                WGPUMultisampleState    PipelineMultisampleState = {};

                ImGui_ImplWGPU_InitInfo()
                {
                    PipelineMultisampleState.count = 1;
                    PipelineMultisampleState.mask = -1u;
                    PipelineMultisampleState.alphaToCoverageEnabled = false;
                }
            };
        struct ImGui_ImplWGPU_InitInfo plain
            device : wgpu.Device
            NumFramesInFlight = 3:i32
            RenderTargetFormat = wgpu.TextureFormat.Undefined
            DepthStencilFormat = wgpu.TextureFormat.Undefined
            PipelineMultisampleState =
                wgpu.MultisampleState
                    count = 1
                    mask = -1:u32
                    alphaToCoverageEnabled = false

        # bool ImGui_ImplWGPU_Init(ImGui_ImplWGPU_InitInfo* init_info);
        ImplWGPU_Init := extern 'ImGui_ImplWGPU_Init (function bool (mutable@ ImGui_ImplWGPU_InitInfo))
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

        # bool ImGui_ImplSDL3_InitForVulkan(SDL_Window* window);
        # I'm pretty sure it doesn't actually care about the backend at all
        ImplSDL3_InitForVulkan := extern 'ImGui_ImplSDL3_InitForVulkan (function bool (mutable@ sdl.Window))
        # void ImGui_ImplSDL3_Shutdown();
        ImplSDL3_Shutdown := extern 'ImGui_ImplSDL3_Shutdown (function void)
        # void ImGui_ImplSDL3_NewFrame();
        ImplSDL3_NewFrame := extern 'ImGui_ImplSDL3_NewFrame (function void)
        # bool ImGui_ImplSDL3_ProcessEvent(const SDL_Event* event);
        ImplSDL3_ProcessEvent := extern 'ImGui_ImplSDL3_ProcessEvent (function bool (@ sdl.Event))
        # enum ImGui_ImplSDL3_GamepadMode { ImGui_ImplSDL3_GamepadMode_AutoFirst, ImGui_ImplSDL3_GamepadMode_AutoAll, ImGui_ImplSDL3_GamepadMode_Manual };
        enum ImplSDL3_GamepadMode plain
            AutoFirst
            AutoAll
            Manual
        # IMGUI_IMPL_API void     ImGui_ImplSDL3_SetGamepadMode(ImGui_ImplSDL3_GamepadMode mode, SDL_Gamepad** manual_gamepads_array = NULL, int manual_gamepads_count = -1);
        ImplSDL3_SetGamepadMode := extern 'ImGui_ImplSDL3_SetGamepadMode (function void ImplSDL3_GamepadMode (@ (@ sdl.Gamepad)) i32)
        inline... ImplSDL3_SetGamepadMode (mode : ImplSDL3_GamepadMode, manual-gamepads-array : (@ (@ sdl.Gamepad)) = null, manual-gamepads-count : i32 = -1)
            ImplSDL3_SetGamepadMode *...

        local-scope;
