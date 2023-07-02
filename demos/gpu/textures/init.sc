using import Array
using import Option
using import struct
using import String

bottle := __env.bottle
import ...demo-common

using bottle.gpu.types
using bottle.types
using bottle.enums

struct RendererState
    pipeline : RenderPipeline
    bind-group : BindGroup

global render-state : (Option RendererState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "low level texture setup"

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

        let image-data =
            bottle.asset.load-image "linus.jpg"

        my-texture := Texture (copy image-data.width) (copy image-data.height) none (image-data = image-data)
        texture-view := TextureView my-texture
        bind-group := BindGroup ('get-bind-group-layout pipeline 0) (Sampler) texture-view

        render-state =
            RendererState pipeline bind-group
    else ()

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap render-state
    rp := RenderPass (bottle.gpu.get-cmd-encoder) (ColorAttachment (bottle.gpu.get-swapchain-image) (clear? = false))

    'set-pipeline rp ctx.pipeline
    'set-bind-group rp 0 ctx.bind-group
    'draw rp 6
    'finish rp
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
