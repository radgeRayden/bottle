import sdl
import ..gpu
import ..window
from (import ..config) let if-module-enabled

wgpu := import ..gpu.wgpu
ig   := import .bindings

@@ if-module-enabled 'imgui
fn init ()
    using import ..gpu.common

    ig.CreateContext null
    assert
        ig.ImplSDL2_InitForVulkan (window.get-handle)
    assert
        ig.ImplWGPU_Init istate.device 3 (get-preferred-surface-format) wgpu.TextureFormat.Undefined

    io := (ig.GetIO)
    ()

@@ if-module-enabled 'imgui
fn begin-frame ()
    ig.ImplSDL2_NewFrame;
    ig.ImplWGPU_NewFrame;
    ig.NewFrame;

global reset : bool
@@ if-module-enabled 'imgui
fn reset-gpu-state ()
    reset = true
    ()

@@ if-module-enabled 'imgui
fn process-event (event)
    result := ig.ImplSDL2_ProcessEvent event # do we even use this result for anything?
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

global recreate-objects? : bool
@@ if-module-enabled 'imgui
fn render ()
    using gpu.types

    ig.Render;
    if (not reset)
        render-pass := RenderPass (gpu.get-cmd-encoder) (ColorAttachment (gpu.get-swapchain-image) (clear? = false))
        ig.ImplWGPU_RenderDrawData (ig.GetDrawData) render-pass
        'finish render-pass
    else
        ig.ImplWGPU_InvalidateDeviceObjects;
        ig.ImplWGPU_CreateDeviceObjects;
        reset = false

@@ if-module-enabled 'imgui
fn shutdown ()
    ig.ImplSDL2_Shutdown;
    ig.ImplWGPU_Shutdown;
    ig.Shutdown;

@@ if-module-enabled 'imgui
fn end-frame ()
    ig.EndFrame;

..
    do
        let init begin-frame reset-gpu-state process-event render shutdown end-frame
        local-scope;
    ig
