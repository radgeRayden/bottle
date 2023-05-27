import wgpu

do
    FrontFace := wgpu.FrontFace
    PrimitiveTopology := wgpu.PrimitiveTopology
    ShaderStage   := wgpu.ShaderStage
    TextureFormat := wgpu.TextureFormat

    from (import ..gpu.ShaderModule) let ShaderLanguage
    from (import .keyconstants) let KeyboardKey
    from (import .mousebuttons) let MouseButton
    from (import .controllerconstants) let ControllerAxis ControllerButton

    locals;
