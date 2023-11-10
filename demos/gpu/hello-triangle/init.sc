using import struct
using import Option
import ...demo-common

import bottle

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

using bottle.types
using bottle.enums

struct RendererState
    pipeline : RenderPipeline

global render-state : (Option RendererState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "hello, triangle!"
    cfg.gpu.msaa-samples = 4
    cfg.enabled-modules.plonk = false
    ;

@@ 'on bottle.load
fn ()
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
                        module = vert
                        "main"
                fragment-stage =
                    FragmentStage
                        module = frag
                        "main"
                        color-targets =
                            typeinit
                                ColorTarget
                                    format = (bottle.gpu.get-preferred-surface-format)
                msaa-samples = (bottle.gpu.get-msaa-sample-count)
        render-state =
            RendererState
                pipeline = pipeline

    else ()

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap render-state

    rp :=
        RenderPass (bottle.gpu.get-cmd-encoder)
            ColorAttachment (bottle.gpu.get-msaa-resolve-source)
                resolve-target = (bottle.gpu.get-surface-texture)
                clear? = false
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
