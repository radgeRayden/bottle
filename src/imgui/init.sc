import sdl
import ..window

wgpu := import ..gpu.wgpu
ig   := import .bindings

fn init ()
    using import ..gpu.common

    ig.CreateContext null
    assert
        ig.ImplSDL2_InitForVulkan (window.get-handle)
    assert
        ig.ImplWGPU_Init istate.device 3 (get-preferred-surface-format) wgpu.TextureFormat.Undefined

    io := (ig.GetIO)
    io.IniFilename = null
    ()

global reset : bool
fn begin-frame ()
    if reset
        ig.ImplWGPU_CreateDeviceObjects;
        reset = false
        return;

    ig.ImplSDL2_NewFrame;
    ig.ImplWGPU_NewFrame;
    ig.NewFrame;

fn wgpu-reset ()
    ig.ImplWGPU_InvalidateDeviceObjects;
    reset = true

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

fn render (render-pass)
    ig.Render;
    ig.ImplWGPU_RenderDrawData (ig.GetDrawData) (view render-pass)

fn shutdown ()
    ig.ImplSDL2_Shutdown;
    ig.ImplWGPU_Shutdown;
    ig.Shutdown;

fn end-frame ()
    ig.EndFrame;

..
    do
        let init begin-frame wgpu-reset process-event render shutdown end-frame
        local-scope;
    ig
