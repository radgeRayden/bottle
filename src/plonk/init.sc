using import glm
using import Option
using import String
using import struct

using import .common
using import ..gpu.types
using import ..enums
using import .SpriteAtlas
using import .GeometryBatch
import ..asset
import ..callbacks
import ..gpu
import ..math
import ..window
import .shaders

struct PlonkState
    batch : GeometryBatch
    sampler : Sampler
    default-texture-binding : BindGroup
    render-pass : (Option RenderPass)

    last-texture    : u64
    default-texture : u64

global context : (Option PlonkState)

fn init ()
    try # none of this is supposed to fail. If it does, we will crash as we should when trying to unwrap state.
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
            PlonkState
                default-texture-binding = (BindGroup ('get-bind-group-layout sprite-pipeline 1) (view sampler) (view default-sprite))
                sampler = sampler
                batch =
                    GeometryBatch
                        attribute-buffer = typeinit 4096
                        index-buffer = typeinit 8192
                        uniform-buffer = typeinit 1
                        pipeline = sprite-pipeline

    else ()

fn begin-frame ()
    ctx := 'force-unwrap context

    w h := (window.get-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'frame-write ctx.batch.uniform-buffer (Uniforms mvp)

    cmd-encoder := (gpu.get-cmd-encoder)
    swapchain-image := (gpu.get-swapchain-image)
    ctx.last-texture = ('get-id swapchain-image)
    rp := RenderPass cmd-encoder (ColorAttachment swapchain-image false)
    'set-bind-group rp 1 ctx.default-texture-binding

    ctx.render-pass = rp

fn set-texture (ctx bind-group id)
    if (ctx.last-texture != id)
        rp := ('force-unwrap ctx.render-pass)

        if (ctx.last-texture != 0)
            'flush ctx.batch rp

        'set-bind-group rp 1 bind-group
        ctx.last-texture = id

fn... sprite (atlas : SpriteAtlas, ...)
    ctx := 'force-unwrap context

    if (not atlas.bind-group)
        atlas.bind-group = BindGroup ('get-bind-group-layout ctx.batch.pipeline 1) ctx.sampler atlas.texture-view

    set-texture ctx ('force-unwrap atlas.bind-group) ('get-id atlas.texture-view)
    'add-quad ctx.batch ...

fn rectangle (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-quad ctx.batch ...

fn circle (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-circle ctx.batch ...

fn polygon (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-polygon ctx.batch ...

fn submit ()
    ctx := 'force-unwrap context

    rp := ('force-unwrap ('swap ctx.render-pass none))
    'finish ctx.batch rp
    'finish rp

do
    let init begin-frame sprite rectangle circle polygon submit
    let SpriteAtlas
    local-scope;
