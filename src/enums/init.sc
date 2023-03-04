import wgpu

do
    let GPUTextureFormat = wgpu.TextureFormat

    from (import .keyconstants) let KeyboardKey
    from (import .mousebuttons) let MouseButton
    from (import .controllerconstants) let ControllerAxis ControllerButton

    locals;
