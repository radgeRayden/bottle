using import Array
using import glm
using import String
using import struct

using import ..enums
using import ..gpu.types
using import .common
import .shaders

fn make-pipeline (vshader fshader)
    RenderPipeline
        layout = (nullof PipelineLayout)
        topology = PrimitiveTopology.TriangleList
        winding = FrontFace.CCW
        vertex-stage =
            VertexStage
                shader = vshader
                entry-point = S"main"
        fragment-stage =
            FragmentStage
                shader = fshader
                entry-point = S"main"
                color-targets =
                    arrayof ColorTarget
                        typeinit
                            format = TextureFormat.BGRA8UnormSrgb

struct LineRenderer
    DataBufferType := StorageBuffer LineSegment
    UniformBufferType := UniformBuffer Uniforms

    segment-data : (Array LineSegment)
    outdated?    : bool

    segment-buffer : DataBufferType
    uniform-buffer : UniformBufferType
    segment-pipeline : RenderPipeline
    join-pipeline : RenderPipeline
    bind-group : BindGroup

    inline __typecall (cls)
        frag := ShaderModule shaders.generic-frag ShaderLanguage.SPIRV ShaderStage.Fragment

        segment-pipeline :=
            make-pipeline
                ShaderModule shaders.line-vert ShaderLanguage.SPIRV ShaderStage.Vertex
                frag

        join-pipeline :=
            make-pipeline
                ShaderModule shaders.join-vert ShaderLanguage.SPIRV ShaderStage.Vertex
                frag

        segment-buffer := DataBufferType 4096
        uniform-buffer := UniformBufferType 1
        bind-group := BindGroup ('get-bind-group-layout segment-pipeline 0) (view segment-buffer) (view uniform-buffer)

        super-type.__typecall cls
            segment-buffer = segment-buffer
            uniform-buffer = uniform-buffer
            segment-pipeline = segment-pipeline
            join-pipeline = join-pipeline
            bind-group = bind-group

    fn... add-segments (self, vertices, width : f32 = 1.0, color : vec4 = (vec4 1))
        self.outdated? = true
        for i in (range ((countof vertices) - 1))
            'append self.segment-data
                LineSegment
                    start = vertices @ i
                    end = vertices @ (i + 1)
                    color = color
                    width = width

    fn draw (self render-pass)
        if (not self.outdated?)
            return;

        try
            'frame-write self.segment-buffer self.segment-data
        else ()
        self.outdated? = false

        'set-bind-group render-pass 0 self.bind-group

        segment-count := u32 (countof self.segment-data)
        'set-pipeline render-pass self.segment-pipeline
        'draw render-pass 6 segment-count

        'set-pipeline render-pass self.join-pipeline
        'draw render-pass 3 (segment-count - 1)

        'clear self.segment-data

do
    let LineRenderer
    local-scope;
