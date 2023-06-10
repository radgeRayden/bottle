using import glm
using import Option
using import String
using import struct

using import ...gpu.types
wgpu := import ...gpu.wgpu
using import ...enums
using import .SpriteAtlas
using import .SpriteBatch
import ...gpu
import .shaders
using import .common

struct PlonkSettings plain
    internal-resolution : ivec2

struct PlonkPermanentState
    batch : SpriteBatch
    pipeline : RenderPipeline
    render-target : TextureView
    sampler : Sampler
    clear-color : vec4 = (vec4 1.0 0.017 1.0 1)
    target-binding : BindGroup

struct PlonkFrameState
    render-pass : (Option RenderPass)

    # properties that can break batching
    last-texture    : u64

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

    sprite-vert := ShaderModule shaders.sprite-vert ShaderLanguage.SPIRV ShaderStage.Vertex
    sprite-frag := ShaderModule shaders.sprite-frag ShaderLanguage.SPIRV ShaderStage.Fragment
    context =
        PlonkPermanentState
            pipeline = pipeline
            target-binding = (BindGroup ('get-bind-group-layout pipeline 0) (view sampler) (view texture-view))
            render-target = texture-view
            sampler = sampler
            batch =
                SpriteBatch
                    attribute-buffer = typeinit 4096
                    index-buffer = typeinit 8192
                    uniform-buffer = typeinit 1
                    pipeline =
                        RenderPipeline
                            layout = (nullof PipelineLayout)
                            topology = PrimitiveTopology.TriangleList
                            winding = FrontFace.CCW
                            vertex-stage =
                                VertexStage
                                    shader = sprite-vert
                                    entry-point = S"main"
                            fragment-stage =
                                FragmentStage
                                    shader = sprite-frag
                                    entry-point = S"main"
                                    color-targets =
                                        arrayof ColorTarget
                                            typeinit
                                                format = TextureFormat.BGRA8UnormSrgb
    settings =
        typeinit
            internal-resolution = (ivec2 width height)

fn begin-frame ()
    ctx := 'force-unwrap context
    color-attachment := ColorAttachment ctx.render-target ctx.clear-color

    inline ortho (width height)
        # https://www.scratchapixel.com/lessons/3d-basic-rendering/perspective-and-orthographic-projection-matrix/orthographic-projection-matrix
        # right, top
        let r t = (width / 2) (height / 2)
        # left, bottom
        let l b = -r -t
        # far, near
        let f n = 100 -100
        mat4
            vec4 (2 / (r - l), 0.0, 0.0, -((r + l) / (r - l)))
            vec4 (0.0, 2 / (t - b), 0.0, 0.0)
            vec4 (-((t + b) / (t - b)), 0.0, -2 / (f - n), -((f + n) / (f - n)))
            vec4 0.0 0.0 0.0 1.0
    'frame-write ctx.batch.uniform-buffer (Uniforms (ortho (unpack settings.internal-resolution)))

    cmd-encoder := (gpu.get-cmd-encoder)
    frame-context =
        PlonkFrameState
            render-pass = RenderPass cmd-encoder color-attachment

fn sprite (atlas position size color)
    ctx := 'force-unwrap context
    frame-ctx := 'force-unwrap frame-context

    if (frame-ctx.last-texture != ('get-id atlas.texture-view))
        rp := ('force-unwrap frame-ctx.render-pass)
        'flush ctx.batch rp
        if (not atlas.bind-group)
            atlas.bind-group = BindGroup ('get-bind-group-layout ctx.batch.pipeline 1) ctx.sampler atlas.texture-view
        'set-bind-group rp 1 ('force-unwrap atlas.bind-group)
        frame-ctx.last-texture = ('get-id atlas.texture-view)

    'add-sprite ctx.batch position size ('get-quad atlas) color

fn submit (render-pass)
    ctx := 'force-unwrap context
    frame-ctx := 'force-unwrap frame-context

    'finish ctx.batch ('force-unwrap frame-ctx.render-pass)
    'finish ('force-unwrap ('swap frame-ctx.render-pass none))

    'set-pipeline render-pass ctx.pipeline
    'set-bind-group render-pass 0 ctx.target-binding
    'draw render-pass 6

do
    let init begin-frame sprite submit
    let SpriteAtlas
    local-scope;
