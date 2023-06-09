using import glm
using import Option
using import String
using import struct

using import ...gpu.types
wgpu := import ...gpu.wgpu
using import ...enums
using import .SpriteBatch
import ...gpu
import .shaders

struct PlonkSettings plain
    internal-resolution : ivec2

struct PlonkPermanentState
    # batch : SpriteBatch
    pipeline : RenderPipeline
    render-target : TextureView
    sampler : Sampler
    clear-color : vec4 = (vec4 1.0 0.017 1.0 1)
    target-binding : BindGroup

struct PlonkFrameState
    render-pass : (Option RenderPass)

    # properties that can break batching
    last-texture    : u64
    # texture-binding : BindGroup

global settings : PlonkSettings
global context : (Option PlonkPermanentState)
global frame-context : (Option PlonkFrameState)

fn init (width height filtering)
    vert := ShaderModule shaders.display-vert ShaderLanguage.SPIRV ShaderStage.Vertex
    frag := ShaderModule shaders.display-frag ShaderLanguage.SPIRV ShaderStage.Fragment
    sampler := (Sampler)
    texture-view := TextureView (Texture (u32 width) (u32 height) TextureFormat.BGRA8UnormSrgb (render-target? = true))
    pipeline :=
        RenderPipeline
            layout = (nullof PipelineLayout)
            topology = PrimitiveTopology.TriangleList
            winding = FrontFace.CCW
            vertex-stage =
                VertexStage
                    shader = vert
                    entry-point = S"main"
            fragment-stage =
                FragmentStage
                    shader = frag
                    entry-point = S"main"
                    color-targets =
                        arrayof ColorTarget
                            typeinit
                                format = TextureFormat.BGRA8UnormSrgb

    context =
        PlonkPermanentState
            pipeline = pipeline
            target-binding = (BindGroup ('get-bind-group-layout pipeline 0) (view sampler) (view texture-view))
            render-target = texture-view
            sampler = sampler

fn begin-frame ()
    ctx := 'force-unwrap context
    color-attachment := ColorAttachment ctx.render-target ctx.clear-color

    cmd-encoder := (gpu.get-cmd-encoder)
    frame-context =
        PlonkFrameState
            render-pass = RenderPass cmd-encoder color-attachment

fn sprite (atlas x y color)
    ctx := 'force-unwrap context
    'add-sprite ctx.batch atlas.texture-view x y ('get-quad atlas) color

fn submit (render-pass)
    ctx := 'force-unwrap context
    frame-ctx := 'force-unwrap frame-context
    'finish ('force-unwrap ('swap frame-ctx.render-pass none))
    'set-pipeline render-pass ctx.pipeline
    'set-bind-group render-pass 0 ctx.target-binding
    'draw render-pass 6

do
    let init begin-frame sprite submit
    local-scope;
