import wgpu

do
    TextureFormat := wgpu.TextureFormat
    ShaderStage   := wgpu.ShaderStage

    from (import ..gpu.ShaderModule) let ShaderLanguage
    from (import .keyconstants) let KeyboardKey
    from (import .mousebuttons) let MouseButton
    from (import .controllerconstants) let ControllerAxis ControllerButton

    locals;
