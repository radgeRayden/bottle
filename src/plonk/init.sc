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
import ..asset
import ..gpu
import ..math
import ..window
import .shaders
from (import ..config) let if-module-enabled

struct PlonkState
    sampler : Sampler
    default-texture : TextureView
    default-texture-binding : BindGroup
    render-pass : (Option RenderPass)

    geometry-batch : GeometryBatch
    last-texture    : u64

global context : (Option PlonkState)

@@ if-module-enabled 'plonk
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
    else ()

@@ if-module-enabled 'plonk
fn begin-frame ()
    ctx := 'force-unwrap context
    'begin-frame ctx.geometry-batch

    w h := (window.get-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'set-projection ctx.geometry-batch mvp

    cmd-encoder := (gpu.get-cmd-encoder)
    surface-texture := (gpu.get-surface-texture)
    ctx.last-texture = 0
    let rp =
        if (not (gpu.msaa-enabled?))
            RenderPass cmd-encoder (ColorAttachment surface-texture none false)
        else
            resolve-source := (gpu.get-msaa-resolve-source)
            RenderPass cmd-encoder (ColorAttachment resolve-source surface-texture false)
    'set-bind-group rp 1 ctx.default-texture-binding

    ctx.render-pass = rp

fn set-texture (ctx bind-group texture-view)
    texture-id := ('get-id texture-view)
    rp := ('force-unwrap ctx.render-pass)

    if (ctx.last-texture != texture-id)
        'flush ctx.geometry-batch rp

    'set-bind-group rp 1 bind-group
    ctx.last-texture = texture-id

fn... sprite (atlas : SpriteAtlas, ...)
    ctx := 'force-unwrap context

    if (not atlas.bind-group)
        atlas.bind-group = BindGroup ('get-bind-group-layout ctx.geometry-batch.pipeline 1) ctx.sampler atlas.texture-view

    set-texture ctx ('force-unwrap atlas.bind-group) atlas.texture-view
    'add-quad ctx.geometry-batch ...

fn... rectangle (position, size, rotation = 0:f32, color = (vec4 1))
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-quad ctx.geometry-batch position size rotation (color = color)

fn circle (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-circle ctx.geometry-batch ...

fn polygon (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-polygon ctx.geometry-batch ...

fn... line (vertices, width : f32 = 1.0, color : vec4 = (vec4 1),
            join-kind : LineJoinKind = LineJoinKind.Bevel,
            cap-kind : LineCapKind = LineCapKind.Butt)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-line ctx.geometry-batch *...

fn rectangle-line (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-rectangle-line ctx.geometry-batch ...

fn polygon-line (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-polygon-line ctx.geometry-batch ...

fn circle-line (...)
    ctx := 'force-unwrap context
    set-texture ctx ctx.default-texture-binding ctx.default-texture
    'add-circle-line ctx.geometry-batch ...

@@ if-module-enabled 'plonk
fn submit ()
    ctx := 'force-unwrap context
    rp := 'force-unwrap ('swap ctx.render-pass none)
    'finish ctx.geometry-batch rp
    'finish rp

do
    let init begin-frame sprite rectangle rectangle-line circle circle-line polygon polygon-line line submit
    let SpriteAtlas Quad LineJoinKind LineCapKind
    local-scope;
