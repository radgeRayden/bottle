using import Array
using import glm
using import String
using import struct

using import ..enums
using import ..gpu.types
using import ..helpers
using import .common
import ..gpu
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
        msaa-samples = (gpu.get-msaa-sample-count)

struct LineRenderer
    SegmentBufferType := StorageBuffer LineSegment
    LineBufferType    := StorageBuffer LineData
    UniformBufferType := UniformBuffer PlonkUniforms

    uniforms : PlonkUniforms
    segment-data : (Array LineSegment)
    line-data    : (Array LineData)
    outdated?    : bool

    segment-buffer : SegmentBufferType
    line-buffer    : LineBufferType
    buffer-offset : usize
    uniform-buffer : UniformBufferType
    segment-pipeline : RenderPipeline
    join-pipeline : RenderPipeline
    bind-group : BindGroup

    # FIXME: this is a workaround for a lifetime issue. Review if this is
    # really necessary.
    obsolete-bindgroups : (Array BindGroup)
    obsolete-buffers : (Array SegmentBufferType)

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

        segment-buffer := SegmentBufferType 4096
        line-buffer    := LineBufferType 10000 # ought to be enough draw calls in a single frame for everyone
        uniform-buffer := UniformBufferType 1
        bind-group := BindGroup ('get-bind-group-layout segment-pipeline 0) (view uniform-buffer) (view segment-buffer) (view line-buffer)

        super-type.__typecall cls
            segment-buffer = segment-buffer
            line-buffer = line-buffer
            uniform-buffer = uniform-buffer
            segment-pipeline = segment-pipeline
            join-pipeline = join-pipeline
            bind-group = bind-group

    fn set-projection (self mvp)
        self.uniforms.mvp = mvp

    fn begin-frame (self)
        'clear self.obsolete-bindgroups
        'clear self.obsolete-buffers
        'frame-write self.uniform-buffer self.uniforms

    fn... add-segments (self, vertices, width : f32 = 1.0, color : vec4 = (vec4 1),
                        join-kind : LineJoinKind = LineJoinKind.Bevel,
                        cap-kind : LineCapKind = LineCapKind.Butt)
        self.outdated? = true
        for i in (range ((countof vertices) - 1))
            'append self.segment-data
                LineSegment
                    start = vertices @ i
                    end = vertices @ (i + 1)
                    line-index = (countof self.line-data) as u32

        let semicircle-segments =
            if (join-kind == LineJoinKind.Round or cap-kind == LineCapKind.Round)
                25:u32 # TODO: actually calculate this
            else
                0:u32

        'append self.line-data
            LineData
                join-kind = join-kind
                cap-kind = cap-kind
                semicircle-segments = semicircle-segments
                color = color
                width = width

    fn draw (self render-pass)
        if (not self.outdated?)
            return;

        try
            'frame-write self.segment-buffer self.segment-data self.buffer-offset
        else
            # resize then try again
            'append self.obsolete-buffers
                popswap
                    self.segment-buffer
                    'clone self.segment-buffer (self.segment-buffer.Capacity * 2:usize)
            'append self.obsolete-bindgroups
                popswap
                    self.bind-group
                    BindGroup ('get-bind-group-layout self.segment-pipeline 0) (view self.uniform-buffer) (view self.segment-buffer) (view self.line-buffer)
            return (this-function self render-pass)

        self.outdated? = false

        'set-bind-group render-pass 0 self.bind-group

        segment-count := u32 (countof self.segment-data)
        'set-pipeline render-pass self.segment-pipeline
        'draw render-pass 6 segment-count 0:u32 (u32 self.buffer-offset)

        'set-pipeline render-pass self.join-pipeline
        let join-vertex-count =
            switch (('last self.line-data) . join-kind)
            case LineJoinKind.Bevel 3:u32
            case LineJoinKind.Miter 6:u32
            case LineJoinKind.Round (25:u32 * 3) # FIXME: calculate this
            default (unreachable)
        'draw render-pass join-vertex-count (segment-count - 1) 0:u32 (u32 self.buffer-offset)

        self.buffer-offset += (countof self.segment-data)
        'clear self.segment-data

    fn finish (self)
        self.buffer-offset = 0
        try
            'frame-write self.line-buffer self.line-data
        else ()
        'clear self.line-data
do
    let LineRenderer
    local-scope;
