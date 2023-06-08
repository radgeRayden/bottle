import .wgpu

..
    import .BindGroup
    import .CommandEncoder
    import .GPUBuffer
    import .RendererBackendInfo
    import .RenderPass
    import .RenderPipeline
    import .ShaderModule
    import .Texture
    import .Sampler
    do
        ShaderStageFlags := wgpu.ShaderStageFlags
        local-scope;
