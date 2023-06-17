using import struct
using import Option
#using import compiler.Printer
import ...demo-common

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
    ;

@@ 'on bottle.load
fn ()
    # print (repr ((RendererBackendInfo) . RendererString))

    try # resource creation can fail, but in this simple case we don't need to handle it.
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

    else ()

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap render-state

    rp := RenderPass (bottle.gpu.get-cmd-encoder) (ColorAttachment (bottle.gpu.get-swapchain-image) false)
    'set-pipeline rp ctx.pipeline
    'draw rp 3
    'finish rp
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
