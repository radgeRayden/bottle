using import glm
using import Option
using import String
using import struct

wgpu := import ...gpu.wgpu

using import .common
using import ...gpu.types
using import ...enums
using import .SpriteAtlas
using import .SpriteBatch
import ...asset
import ...gpu
import ...math
import ...window
import .shaders

struct PlonkPermanentState
    batch : SpriteBatch
    sampler : Sampler
    default-texture-binding : BindGroup

struct PlonkFrameState
    render-pass : (Option RenderPass)
    # properties that can break batching
    last-texture    : u64

global context : (Option PlonkPermanentState)
global frame-context : (Option PlonkFrameState)

fn init ()
    sampler := (Sampler)
    sprite-vert := ShaderModule shaders.sprite-vert ShaderLanguage.SPIRV ShaderStage.Vertex
    sprite-frag := ShaderModule shaders.sprite-frag ShaderLanguage.SPIRV ShaderStage.Fragment
    sprite-pipeline :=
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

    local default-sprite-imdata : asset.ImageData 1 1
    for byte in default-sprite-imdata.data
        byte = 0xFF

    default-sprite := TextureView (Texture default-sprite-imdata)
    context =
        PlonkPermanentState
            default-texture-binding = (BindGroup ('get-bind-group-layout sprite-pipeline 1) (view sampler) (view default-sprite))
            sampler = sampler
            batch =
                SpriteBatch
                    attribute-buffer = typeinit 4096
                    index-buffer = typeinit 8192
                    uniform-buffer = typeinit 1
                    pipeline = sprite-pipeline

    ()

fn begin-frame ()
    ctx := 'force-unwrap context

    w h := (window.get-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'frame-write ctx.batch.uniform-buffer (Uniforms mvp)

    cmd-encoder := (gpu.get-cmd-encoder)
    frame-context =
        PlonkFrameState
            render-pass = RenderPass cmd-encoder (ColorAttachment (gpu.get-swapchain-image) false)

fn sprite (atlas position size color)
    ctx := 'force-unwrap context
    frame-ctx := 'force-unwrap frame-context

    if (frame-ctx.last-texture != ('get-id atlas.texture-view))
        rp := ('force-unwrap frame-ctx.render-pass)

        if (frame-ctx.last-texture != 0)
            'flush ctx.batch rp

        if (not atlas.bind-group)
            atlas.bind-group = BindGroup ('get-bind-group-layout ctx.batch.pipeline 1) ctx.sampler atlas.texture-view
        'set-bind-group rp 1 ('force-unwrap atlas.bind-group)
        frame-ctx.last-texture = ('get-id atlas.texture-view)

    'add-sprite ctx.batch position size ('get-quad atlas) color

fn submit (render-pass)
    ctx := 'force-unwrap context
    frame-ctx := 'force-unwrap frame-context

    frame-rp := ('force-unwrap ('swap frame-ctx.render-pass none))
    if (frame-ctx.last-texture == 0)
        'set-bind-group frame-rp 1 ctx.default-texture-binding

    'finish ctx.batch frame-rp
    'finish frame-rp

do
    let init begin-frame sprite submit
    let SpriteAtlas
    local-scope;
