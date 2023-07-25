using import Array enum String struct
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

    ShaderStageFlags := wgpu.ShaderStageFlags
    Sampler := GPUSampler
    unlet GPUSampler
    local-scope;
