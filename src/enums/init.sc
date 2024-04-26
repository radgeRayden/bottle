wgpu := import ..gpu.wgpu

do
    FilterMode := wgpu.FilterMode
    FrontFace := wgpu.FrontFace
    PowerPreference := wgpu.PowerPreference
    PresentMode := wgpu.PresentMode
    PrimitiveTopology := wgpu.PrimitiveTopology
    ShaderStage   := wgpu.ShaderStage
    TextureFormat := wgpu.TextureFormat
    WrapMode := wgpu.AddressMode

    from (import ..gpu.types) let ShaderLanguage
    from (import .keyconstants) let KeyboardKey
    from (import .mousebuttons) let MouseButton
    from (import .controllerconstants) let ControllerAxis ControllerButton
    from (import ..filesystem.FileStream) let FileMode

    locals;
