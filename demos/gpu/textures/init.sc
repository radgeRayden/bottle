using import Array
using import Option
using import struct
using import String
#using import compiler.Printer

stbi := import stb.image
bottle := __env.bottle
using bottle.gpu.types
using bottle.types
using bottle.enums

struct RendererState
    pipeline : RenderPipeline
    bind-group : BindGroup

global render-state : (Option RendererState)

fn load-image (filename)
    local w : i32
    local h : i32
    local channels : i32

    data := stbi.load filename &w &h &channels 4
    assert (data != null)

    data := 'wrap (Array u8) data (w * h * 4)
    ImageData (w as u32) (h as u32) (slices = 1:u32) (data = data)

@@ 'on bottle.load
fn ()
    # print ((RendererBackendInfo) . RendererString)

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

        image-data := load-image (.. module-dir "/linus.jpg")
        my-texture := Texture (copy image-data.width) (copy image-data.height) none (image-data = image-data)
        texture-view := TextureView my-texture
        bind-group := BindGroup ('get-bind-group-layout pipeline 0) (Sampler) texture-view

        render-state =
            RendererState pipeline bind-group
    else ()

@@ 'on bottle.render
fn (render-pass)
    rp  := render-pass
    ctx := 'force-unwrap render-state

    'set-pipeline rp ctx.pipeline
    'set-bind-group rp 0 ctx.bind-group
    'draw rp 6

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
