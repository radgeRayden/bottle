using import struct
using import Option

bottle := __env.bottle

fn shaderf-vert ()
    using import glsl
    using import glm

    out vcolor : vec4
        location = 0

    local vertices =
        arrayof vec3
            vec3 0.0 0.5 0.0
            vec3 -0.5 -0.5 0.0
            vec3 0.5 -0.5 0.0

    local colors =
        arrayof vec4
            vec4 1.0 0.0 0.0 1.0
            vec4 0.0 1.0 0.0 1.0
            vec4 0.0 0.0 1.0 1.0

    idx := gl_VertexIndex
    vcolor = (colors @ idx)
    gl_Position = (vec4 (vertices @ idx) 1.0)

fn shaderf-frag ()
    using import glsl
    using import glm

    in vcolor : vec4
        location = 0
    out fcolor : vec4
        location = 0
    fcolor = vcolor

using bottle.gpu.types
using bottle.enums

struct RendererState
    pipeline : RenderPipeline

global render-state : (Option RendererState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "hello, triangle!"
    cfg.window.width = 800
    cfg.window.height = 600
    ;

@@ 'on bottle.load
fn ()
    vert := ShaderModule shaderf-vert ShaderLanguage.SPIRV ShaderStage.Vertex
    frag := ShaderModule shaderf-frag ShaderLanguage.SPIRV ShaderStage.Fragment

    pipeline :=
        RenderPipeline
            layout = (PipelineLayout)
            topology = PrimitiveTopology.TriangleList
            winding = FrontFace.CCW
            vertex-stage =
                VertexStage
                    shader = vert
                    "main"
            fragment-stage =
                FragmentStage
                    shader = frag
                    "main"
                    color-targets =
                        arrayof ColorTarget
                            typeinit
                                format = (bottle.gpu.get-preferred-surface-format)
    render-state =
        RendererState
            pipeline = pipeline
    ;

@@ 'on bottle.render
fn (render-pass)
    rp  := render-pass
    ctx := 'force-unwrap render-state

    'set-pipeline rp ctx.pipeline
    'cmd-draw rp 3
    ;

bottle.run;
