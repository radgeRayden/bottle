using import struct
using import Option

bottle := __env.bottle

let shader =
    """"struct VertexOutput {
            @location(0) vcolor: vec4<f32>,
            @builtin(position) position: vec4<f32>,
        };

        var<private> vertices : array<vec3<f32>, 3u> = array<vec3<f32>, 3u>(
            vec3<f32>(0.0, 0.5, 0.0),
            vec3<f32>(-0.5, -0.5, 0.0),
            vec3<f32>(0.5, -0.5, 0.0),
        );

        var<private> colors : array<vec4<f32>, 3u> = array<vec4<f32>, 3u>(
            vec4<f32>(1.0, 0.0, 0.0, 1.0),
            vec4<f32>(0.0, 1.0, 0.0, 1.0),
            vec4<f32>(0.0, 0.0, 1.0, 1.0),
        );

        @vertex
        fn vs_main(@builtin(vertex_index) vindex: u32) -> VertexOutput {
            var out: VertexOutput;
            out.position = vec4<f32>(vertices[vindex], 1.0);
            out.vcolor = colors[vindex];
            return out;
        }

        @fragment
        fn fs_main(vertex: VertexOutput) -> @location(0) vec4<f32> {
            return vertex.vcolor;
        }

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

    wgsl := ShaderModule shader ShaderLanguage.WGSL
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
