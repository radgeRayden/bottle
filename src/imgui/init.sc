import ..gpu ..window
using import ..gpu.common ..context radl.ext

wgpu := import ..gpu.wgpu
ig   := import .bindings
sdl  := import sdl3

wgpu-device := context-accessor 'gpu 'device
scaled-surface-size := context-accessor 'gpu 'scaled-surface-size

@@ if-module-enabled 'imgui
fn init ()
    ig.CreateContext null
    assert
        ig.ImplSDL3_InitForVulkan (window.get-handle)
    assert
        do
            local info =
                ig.ImGui_ImplWGPU_InitInfo
                    (wgpu-device)
                    RenderTargetFormat = (gpu.get-preferred-surface-format)
            ig.ImplWGPU_Init &info
    ()

@@ if-module-enabled 'imgui
fn begin-frame ()
    ig.ImplSDL3_NewFrame;
    ig.ImplWGPU_NewFrame;
    ig.NewFrame;

global reset : bool
@@ if-module-enabled 'imgui
fn reset-gpu-state ()
    reset = true
    ()

@@ if-module-enabled 'imgui
fn process-event (event)
    result := ig.ImplSDL3_ProcessEvent event # do we even use this result for anything?
    io := (ig.GetIO)

    switch (event.type as sdl.EventType)
    pass 'MOUSE_MOTION
    pass 'MOUSE_BUTTON_DOWN
    pass 'MOUSE_BUTTON_UP
    pass 'MOUSE_WHEEL
    do (deref io.WantCaptureMouse)
    pass 'KEY_DOWN
    pass 'KEY_UP
    do (deref io.WantCaptureKeyboard)
    default false #result

global recreate-objects? : bool
@@ if-module-enabled 'imgui
fn render ()
    using gpu.types

    ig.Render;
    if (not reset)
        render-pass := RenderPass (gpu.get-cmd-encoder) (ColorAttachment (gpu.get-surface-texture) (clear? = false))
        draw-data := (ig.GetDrawData)
        draw-data.DisplaySize = ig.Vec2 (|> f32 (unpack (scaled-surface-size)))
        ig.ImplWGPU_RenderDrawData draw-data render-pass
        'finish render-pass
    else
        ig.ImplWGPU_InvalidateDeviceObjects;
        ig.ImplWGPU_CreateDeviceObjects;
        reset = false

@@ if-module-enabled 'imgui
fn shutdown ()
    ig.ImplSDL3_Shutdown;
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
