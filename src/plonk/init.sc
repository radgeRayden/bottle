using import glm
using import Option
using import String
using import struct

using import .common
using import ..gpu.types
using import ..enums
using import .SpriteAtlas
using import .GeometryBatch
using import .LineRenderer
import ..asset
import ..gpu
import ..math
import ..window
import .shaders

struct PlonkState
    batch : GeometryBatch
    line-renderer : LineRenderer
    sampler : Sampler
    default-texture-binding : BindGroup
    render-pass : (Option RenderPass)

    last-texture    : u64
    default-texture : u64

global context : (Option PlonkState)

fn init ()
    try # none of this is supposed to fail. If it does, we will crash as we should when trying to unwrap state.
        sampler := (Sampler)
        local default-texture-imdata : asset.ImageData 1 1
        for byte in default-texture-imdata.data
            byte = 0xFF

        default-texture := TextureView (Texture default-texture-imdata)
        # FIXME: switch to explicit layout
        batch := (GeometryBatch)
        context =
            PlonkState
                default-texture-binding = (BindGroup ('get-bind-group-layout batch.pipeline 1) (view sampler) (view default-texture))
                sampler = sampler
                batch = batch
    else ()

fn begin-frame ()
    ctx := 'force-unwrap context

    w h := (window.get-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'frame-write ctx.batch.uniform-buffer (Uniforms mvp)
    'frame-write ctx.line-renderer.uniform-buffer (Uniforms mvp)

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

fn... rectangle (position, size, rotation, color)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-quad ctx.batch position size rotation (color = color)

fn circle (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-circle ctx.batch ...

fn polygon (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-polygon ctx.batch ...

fn line (vertices)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-segments ctx.line-renderer vertices
    'draw ctx.line-renderer ('force-unwrap ctx.render-pass)

fn submit ()
    ctx := 'force-unwrap context

    rp := ('force-unwrap ('swap ctx.render-pass none))
    'finish ctx.batch rp
    'finish rp

do
    let init begin-frame sprite rectangle circle polygon line submit
    let SpriteAtlas Quad
    local-scope;
