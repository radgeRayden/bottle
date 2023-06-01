using import Array
using import Option
using import String
using import struct
using import .common

bottle := __env.bottle
random := import radl.random

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

QUAD-COUNT  := 120000:u32
fn gen-quads ()
    using import glm
    local quads : (Array Quad)
    'resize quads QUAD-COUNT

    local rng : random.RNG 0
    unit-rand := () -> (random.normalize (rng))
    for quad in quads
        quad.rotation = (2 * pi:f64 * (unit-rand)) as f32
        quad.size = (rng 5 50) as f32
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
                        shader = vert
                        entry-point = S"main"
                fragment-stage =
                    FragmentStage
                        shader = frag
                        entry-point = S"main"
                        color-targets =
                            arrayof ColorTarget
                                typeinit
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

@@ 'on bottle.render
fn (rp)
    using import glm

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

    ctx := 'force-unwrap render-state
    time := (bottle.timer.get-time)
    'frame-write ctx.uniform-buffer (Uniforms (ortho (bottle.window.get-size)) (time as f32))

    'set-pipeline rp ctx.pipeline
    'set-index-buffer rp ctx.index-buffer
    'set-bind-group rp 0 ctx.bind-group

    'draw-indexed rp INDEX-COUNT QUAD-COUNT
    ()

bottle.run;
