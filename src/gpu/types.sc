using import Array enum Map radl.strfmt String struct
import .wgpu

do
    type BindGroupLayout <:: wgpu.BindGroupLayout
    type BindGroup <:: wgpu.BindGroup
    type ColorTarget <: wgpu.ColorTargetState
    type CommandEncoder <:: wgpu.CommandEncoder
    type CommandBuffer <:: wgpu.CommandBuffer
    type GPUBuffer <:: wgpu.Buffer
    type GenericBuffer <:: GPUBuffer
    type StorageBuffer <:: GPUBuffer
    type IndexBuffer <:: GPUBuffer
    type UniformBuffer <:: GPUBuffer
    type ColorAttachment <: wgpu.RenderPassColorAttachment
    type RenderPass <:: wgpu.RenderPassEncoder
    type GPUSampler <:: wgpu.Sampler
    type PipelineLayout <:: wgpu.PipelineLayout
    type RenderPipeline <:: wgpu.RenderPipeline
    type ShaderModule <:: wgpu.ShaderModule
    type Texture <:: wgpu.Texture
    type TextureView <:: wgpu.TextureView

    struct VertexStage
        module : ShaderModule
        entry-point : String

    struct FragmentStage
        module : ShaderModule
        entry-point : String
        color-targets : (Array ColorTarget)

    enum ShaderLanguage plain
        WGSL
        GLSL
        SPIRV

    struct PushConstantLayout
        ranges-map : (Map String wgpu.PushConstantRange)
        ranges-array : (Array wgpu.PushConstantRange)
        next-offset : u32

    ShaderStageFlags := wgpu.ShaderStageFlags
    Sampler := GPUSampler
    unlet GPUSampler

    struct RendererBackendInfo
        struct WGPUVersion
            major : u8
            minor : u8
            patch : u8
            revision : u8

            inline... __typecall (cls, value)
                using import format
                super-type.__typecall cls
                    major = ((value >> 24) & 0xFF) as u8
                    minor = ((value >> 16) & 0xFF) as u8
                    patch = ((value >> 8) & 0xFF) as u8
                    revision = (value & 0xFF) as u8
            case (cls)
                this-function cls 0:u32

            inline __repr (self)
                f"v${self.major}.${self.minor}.${self.patch}.${self.revision}"

        version : WGPUVersion
        vendor : String
        architecture : String
        device : String
        driver : String
        adapter : wgpu.AdapterType
        low-level-backend : wgpu.BackendType

    local-scope;
