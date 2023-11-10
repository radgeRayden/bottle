using import Array
using import Option
using import String
using import struct
using import .common

import bottle
random := bottle.random
import ...demo-common

using bottle.gpu.types
using bottle.enums

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "storage and uniform buffers"
    cfg.window.relative-width = 1
    cfg.window.relative-height = 1
    ;

struct RendererState
    pipeline       : RenderPipeline
    quad-buffer    : (StorageBuffer Quad)
    index-buffer   : (IndexBuffer u32)
    uniform-buffer : (UniformBuffer Uniforms)
    bind-group     : BindGroup

global render-state : (Option RendererState)

QUAD-COUNT  := 4194304:u32
fn gen-quads ()
    using import glm
    local quads : (Array Quad)
    'resize quads QUAD-COUNT

    local rng : random.RNG 0
    unit-rand := () -> (random.normalize (rng))
    for quad in quads
        quad.rotation = (2 * pi:f64 * (unit-rand)) as f32
        quad.size = (rng 1 6) as f32
        quad.color = vec4 (va-map unit-rand (va-range 4))
        quad.speed = (rng 5 250) as f32
        quad.direction = (2 * pi:f64 * (unit-rand)) as f32
    quads

INDEX-COUNT := 6:u32
fn gen-indices ()
    local indices : (Array u32)
    'resize indices INDEX-COUNT
    local base-indices =
        arrayof u32 0 2 3 3 1 0
    for i idx in (enumerate indices)
        offset := i % 6
        idx = base-indices @ offset
    indices

@@ 'on bottle.load
fn ()
    try
        vert := ShaderModule (import .vert) ShaderLanguage.SPIRV ShaderStage.Vertex
        frag := ShaderModule (import .frag) ShaderLanguage.SPIRV ShaderStage.Fragment

        pipeline :=
            RenderPipeline
                layout = (nullof PipelineLayout)
                topology = PrimitiveTopology.TriangleList
                winding = FrontFace.CCW
                vertex-stage =
                    VertexStage
                        module = vert
                        entry-point = "main"
                fragment-stage =
                    FragmentStage
                        module = frag
                        entry-point = S"main"
                        color-targets =
                            typeinit
                                ColorTarget
                                    format = (bottle.gpu.get-preferred-surface-format)

        # TODO: change to map on create
        quad-buffer := (StorageBuffer Quad) QUAD-COUNT
        index-buffer := (IndexBuffer u32) INDEX-COUNT
        uniform-buffer := (UniformBuffer Uniforms) 1

        'frame-write quad-buffer (gen-quads)
        'frame-write index-buffer (gen-indices)

        bind-group := BindGroup ('get-bind-group-layout pipeline 0) quad-buffer uniform-buffer

        render-state =
            RendererState
                pipeline = pipeline
                quad-buffer = quad-buffer
                index-buffer = index-buffer
                uniform-buffer = uniform-buffer
                bind-group = bind-group

    else ()

global time-offset : f64
@@ 'on bottle.key-released
fn (key)
    if (key == KeyboardKey.Space)
        time-offset = (bottle.time.get-time)
    if (key == KeyboardKey.Minus)
        bottle.time.set-global-time-scale
            (bottle.time.get-global-time-scale) - 0.1
    if (key == KeyboardKey.Equals)
        bottle.time.set-global-time-scale
            (bottle.time.get-global-time-scale) + 0.1

@@ 'on bottle.render
fn ()
    using import glm

    ctx := 'force-unwrap render-state
    rp := RenderPass (bottle.gpu.get-cmd-encoder) (ColorAttachment (bottle.gpu.get-surface-texture) (clear? = false))
    time := (bottle.time.get-time) - time-offset
    'frame-write ctx.uniform-buffer (Uniforms (bottle.math.orthographic-projection (bottle.window.get-size)) (time as f32))

    'set-pipeline rp ctx.pipeline
    'set-index-buffer rp ctx.index-buffer
    'set-bind-group rp 0 ctx.bind-group

    'draw-indexed rp INDEX-COUNT QUAD-COUNT
    'finish rp
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
