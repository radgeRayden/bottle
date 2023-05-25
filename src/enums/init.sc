import wgpu

do
    let TextureFormat = wgpu.TextureFormat

    from (import ..gpu.ShaderModule) let ShaderLanguage
    from (import .keyconstants) let KeyboardKey
    from (import .mousebuttons) let MouseButton
    from (import .controllerconstants) let ControllerAxis ControllerButton

    locals;
