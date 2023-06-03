using import .errors
import .wgpu

..
    import .BindGroup
    import .GPUBuffer
    import .RendererBackendInfo
    import .RenderPass
    import .RenderPipeline
    import .ShaderModule
    import .Texture
    do
        let GPUError
        ShaderStageFlags := wgpu.ShaderStageFlags
        local-scope;
