using import .errors
import .wgpu

..
    import .BindGroup
    import .RenderPass
    import .RenderPipeline
    import .ShaderModule
    import .Texture
    import .GPUBuffer
    do
        let GPUError
        ShaderStageFlags := wgpu.ShaderStageFlags
        local-scope;
