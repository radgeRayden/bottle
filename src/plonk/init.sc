using import enum glm Option String struct

using import .common ..context ..gpu.types ..enums .SpriteAtlas .GeometryBatch .TextureBinding
import ..asset ..gpu ..math ..window .shaders

struct PlonkState
    render-pass : (Option RenderPass)
    default-texture-binding : TextureBinding
    geometry-batch : GeometryBatch

global context : (Option PlonkState)

@@ if-module-enabled 'plonk
fn init ()
    try # none of this is supposed to fail. If it does, we will crash as we should when trying to unwrap state.
        local default-texture-imdata = asset.ImageData 1 1
        for byte in default-texture-imdata.data
            byte = 0xFF

        default-texture-binding := TextureBinding (Texture default-texture-imdata)

        context =
            PlonkState
                geometry-batch = (GeometryBatch default-texture-binding)
                default-texture-binding = default-texture-binding
    else ()

@@ if-module-enabled 'plonk
fn begin-frame ()
    ctx := 'force-unwrap context
    'begin-frame ctx.geometry-batch

    w h := (window.get-drawable-size)
    mvp :=
        *
            math.orthographic-projection w h
            math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)
    'set-projection ctx.geometry-batch mvp

    cmd-encoder := (gpu.get-cmd-encoder)
    surface-texture := (gpu.get-surface-texture)
    let rp =
        if (not (gpu.msaa-enabled?))
            RenderPass cmd-encoder (ColorAttachment surface-texture none false)
        else
            resolve-source := (gpu.get-msaa-resolve-source)
            RenderPass cmd-encoder (ColorAttachment resolve-source surface-texture false)
    'set-render-pass ctx.geometry-batch rp
    ctx.render-pass = rp

fn set-texture-binding (ctx texture-binding)
    'set-texture-binding ctx.geometry-batch texture-binding

fn... sprite (binding : TextureBinding, ...)
    ctx := 'force-unwrap context

    set-texture-binding ctx binding
    'add-quad ctx.geometry-batch ...

fn... rectangle (position : vec2, size : vec2, rotation : f32 = 0, color : vec4 = (vec4 1))
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-quad ctx.geometry-batch position size rotation (color = color)

fn circle (...)
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-circle ctx.geometry-batch ...

fn polygon (...)
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-polygon ctx.geometry-batch ...

fn... line (vertices, width : f32 = 1.0, color : vec4 = (vec4 1),
            join-kind : LineJoinKind = LineJoinKind.Bevel,
            cap-kind : LineCapKind = LineCapKind.Butt)
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-line ctx.geometry-batch *...

fn rectangle-line (...)
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-rectangle-line ctx.geometry-batch ...

fn polygon-line (...)
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-polygon-line ctx.geometry-batch ...

fn circle-line (...)
    ctx := 'force-unwrap context
    set-texture-binding ctx ctx.default-texture-binding
    'add-circle-line ctx.geometry-batch ...

@@ if-module-enabled 'plonk
fn submit ()
    ctx := 'force-unwrap context
    rp := 'force-unwrap ('swap ctx.render-pass none)
    'finish ctx.geometry-batch
    'finish rp
    set-texture-binding ctx ctx.default-texture-binding

do
    let init begin-frame sprite rectangle rectangle-line circle circle-line polygon polygon-line line submit
    let SpriteAtlas Quad LineJoinKind LineCapKind TextureBinding
    local-scope;
