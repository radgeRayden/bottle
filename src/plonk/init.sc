using import enum
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

enum BatchType plain
    None
    GenericGeometry
    Lines

struct PlonkState
    sampler : Sampler
    default-texture : TextureView
    default-texture-binding : BindGroup
    render-pass : (Option RenderPass)

    geometry-batch : GeometryBatch
    line-renderer : LineRenderer

    last-texture    : u64

    current-batch : BatchType

global context : (Option PlonkState)

fn init ()
    try # none of this is supposed to fail. If it does, we will crash as we should when trying to unwrap state.
        sampler := (Sampler)
        local default-texture-imdata : asset.ImageData 1 1
        for byte in default-texture-imdata.data
            byte = 0xFF

        default-texture := TextureView (Texture default-texture-imdata)
        # FIXME: switch to explicit layout
        geometry-batch := (GeometryBatch)
        context =
            PlonkState
                default-texture-binding = (BindGroup ('get-bind-group-layout geometry-batch.pipeline 1) (view sampler) (view default-texture))
                default-texture = default-texture
                sampler = sampler
                geometry-batch = geometry-batch
                current-batch = BatchType.None

    else ()

fn begin-frame ()
    ctx := 'force-unwrap context

    w h := (window.get-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'frame-write ctx.geometry-batch.uniform-buffer (Uniforms mvp)
    'frame-write ctx.line-renderer.uniform-buffer (Uniforms mvp)

    cmd-encoder := (gpu.get-cmd-encoder)
    swapchain-image := (gpu.get-swapchain-image)
    ctx.last-texture = 0
    rp := RenderPass cmd-encoder (ColorAttachment swapchain-image false)
    'set-bind-group rp 1 ctx.default-texture-binding

    ctx.render-pass = rp

fn set-batch (kind ctx bind-group texture-view)
    texture-id := ('get-id texture-view)
    rp := ('force-unwrap ctx.render-pass)

    if ((ctx.current-batch != kind) or (ctx.last-texture != texture-id))
        'flush ctx.geometry-batch rp
        'draw ctx.line-renderer rp

    'set-bind-group rp 1 bind-group
    ctx.last-texture = texture-id
    ctx.current-batch = kind

fn... sprite (atlas : SpriteAtlas, ...)
    ctx := 'force-unwrap context

    if (not atlas.bind-group)
        atlas.bind-group = BindGroup ('get-bind-group-layout ctx.geometry-batch.pipeline 1) ctx.sampler atlas.texture-view

    set-batch BatchType.GenericGeometry ctx ('force-unwrap atlas.bind-group) atlas.texture-view
    'add-quad ctx.geometry-batch ...

fn... rectangle (position, size, rotation, color)
    ctx := 'force-unwrap context
    set-batch BatchType.GenericGeometry ctx ctx.default-texture-binding ctx.default-texture
    'add-quad ctx.geometry-batch position size rotation (color = color)

fn circle (...)
    ctx := 'force-unwrap context
    set-batch BatchType.GenericGeometry ctx ctx.default-texture-binding ctx.default-texture
    'add-circle ctx.geometry-batch ...

fn polygon (...)
    ctx := 'force-unwrap context
    set-batch BatchType.GenericGeometry ctx ctx.default-texture-binding ctx.default-texture
    'add-polygon ctx.geometry-batch ...

fn line (...)
    ctx := 'force-unwrap context
    set-batch BatchType.GenericGeometry ctx ctx.default-texture-binding ctx.default-texture
    'add-segments ctx.line-renderer ...

fn submit ()
    ctx := 'force-unwrap context

    rp := ('force-unwrap ctx.render-pass)
    'finish ctx.geometry-batch rp
    set-batch BatchType.None ctx ctx.default-texture-binding ctx.default-texture
    'finish ('force-unwrap ('swap ctx.render-pass none))

do
    let init begin-frame sprite rectangle circle polygon line submit
    let SpriteAtlas Quad
    local-scope;
