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
from (import ..config) let if-module-enabled

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
                current-batch = BatchType.None
    else ()

@@ if-module-enabled 'plonk
fn begin-frame ()
    ctx := 'force-unwrap context
    'begin-frame ctx.line-renderer
    'begin-frame ctx.geometry-batch

    w h := (window.get-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'set-projection ctx.geometry-batch mvp
    'set-projection ctx.line-renderer mvp

    cmd-encoder := (gpu.get-cmd-encoder)
    swapchain-image := (gpu.get-swapchain-image)
    ctx.last-texture = 0
    let rp =
        if (not (gpu.msaa-enabled?))
            RenderPass cmd-encoder (ColorAttachment swapchain-image none false)
        else
            resolve-source := (gpu.get-swapchain-resolve-source)
            RenderPass cmd-encoder (ColorAttachment resolve-source swapchain-image false)
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

fn... line (vertices, width : f32 = 1.0, color : vec4 = (vec4 1),
            join-kind : LineJoinKind = LineJoinKind.Bevel,
            cap-kind : LineCapKind = LineCapKind.Butt)
    ctx := 'force-unwrap context
    set-batch BatchType.Lines ctx ctx.default-texture-binding ctx.default-texture
    'add-segments ctx.line-renderer *...
    'draw ctx.line-renderer ('force-unwrap ctx.render-pass)
    if ((countof vertices) < 2)
        return;

    start end := vertices @ 0, vertices @ ((countof vertices) - 1)
    start+1 end-1 := vertices @ 1, vertices @ ((countof vertices) - 2)
    sangle eangle := atan2 (unpack ((start+1 - start) . yx)), atan2 (unpack ((end - end-1) . yx))
    switch cap-kind
    case LineCapKind.Round
        circle start (width / 2) color
        circle end (width / 2) color
    case LineCapKind.Square
        radius := (width / 2) * (sqrt 2:f32)
        polygon start 4 radius (sangle - (pi / 4)) color
        polygon end 4 radius (eangle - (pi / 4)) color
    default ()

@@ if-module-enabled 'plonk
fn submit ()
    ctx := 'force-unwrap context
    ctx.current-batch = BatchType.None

    rp := 'force-unwrap ('swap ctx.render-pass none)
    'finish ctx.geometry-batch rp
    'finish ctx.line-renderer
    'finish rp

do
    let init begin-frame sprite rectangle circle polygon line submit
    let SpriteAtlas Quad LineJoinKind LineCapKind
    local-scope;
