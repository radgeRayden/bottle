using import Array
using import glm
using import Option
using import String
using import struct

import ..gpu
import ..math
import .shaders
using import ..enums
using import ..gpu.types
using import .common
using import ..helpers

# TODO: unify batch interfaces
struct GeometryBatch
    attribute-buffer : (StorageBuffer VertexAttributes)
    index-buffer     : (IndexBuffer u32)
    uniform-buffer   : (UniformBuffer PlonkUniforms)
    uniforms    : PlonkUniforms
    vertex-data : (Array VertexAttributes)
    index-data  : (Array u32)
    outdated-vertices? : bool
    outdated-indices?  : bool
    vertex-offset : usize
    index-offset  : usize

    pipeline : RenderPipeline
    bind-group : BindGroup
    cached-buffer-id : u64

    # FIXME: this is a workaround for a lifetime issue. Review if this is
    # really necessary.
    obsolete-bindgroups : (Array BindGroup)
    obsolete-buffers : (Array (StorageBuffer VertexAttributes))
    obsolete-index-buffers : (Array (IndexBuffer u32))

    inline __typecall (cls)
        vert := ShaderModule shaders.generic-vert ShaderLanguage.SPIRV ShaderStage.Vertex
        frag := ShaderModule shaders.generic-frag ShaderLanguage.SPIRV ShaderStage.Fragment
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
                                    format = TextureFormat.BGRA8UnormSrgb
                msaa-samples = (gpu.get-msaa-sample-count)

        attrbuf := (StorageBuffer VertexAttributes) 4096
        uniform-buffer := (UniformBuffer PlonkUniforms) 1
        bind-group := BindGroup ('get-bind-group-layout pipeline 0) (view uniform-buffer) (view attrbuf)

        super-type.__typecall cls
            cached-buffer-id = copy ('get-id attrbuf)
            attribute-buffer = attrbuf
            index-buffer = typeinit 8192
            uniform-buffer = uniform-buffer
            pipeline = pipeline
            bind-group = bind-group

    fn set-projection (self mvp)
        self.uniforms.mvp = mvp
        'frame-write self.uniform-buffer self.uniforms

    fn begin-frame (self)
        'clear self.obsolete-bindgroups
        'clear self.obsolete-buffers
        'clear self.obsolete-index-buffers

    fn flush (self render-pass)
        if (not (self.outdated-vertices? or self.outdated-indices?))
            return;

        if self.outdated-vertices?
            attrbuf := self.attribute-buffer
            try
                'frame-write attrbuf self.vertex-data self.vertex-offset
            else
                # resize then try again
                'append self.obsolete-buffers
                    popswap
                        self.attribute-buffer
                        'clone attrbuf (attrbuf.Capacity * 2:usize)
                return (this-function self render-pass)

            if (self.cached-buffer-id != ('get-id attrbuf))
                'append self.obsolete-bindgroups
                    popswap
                        self.bind-group
                        BindGroup ('get-bind-group-layout self.pipeline 0) self.uniform-buffer attrbuf
                self.cached-buffer-id = ('get-id attrbuf)

            self.outdated-vertices? = false

        if self.outdated-indices?
            idxbuf := self.index-buffer
            try
                'frame-write idxbuf self.index-data self.index-offset
            else
                # resize then try again
                'append self.obsolete-index-buffers
                    popswap
                        self.index-buffer
                        'clone idxbuf (idxbuf.Capacity * 2:usize)
                return (this-function self render-pass)

            self.outdated-indices? = false

        index-count := (countof self.index-data)
        'set-index-buffer render-pass self.index-buffer
        'set-pipeline render-pass self.pipeline
        'set-bind-group render-pass 0 self.bind-group
        'draw-indexed render-pass (index-count as u32) 1:u32 (u32 self.index-offset)

        self.vertex-offset += (countof self.vertex-data)
        self.index-offset  += (countof self.index-data)
        'clear self.vertex-data
        'clear self.index-data

    fn... add-quad (self : this-type, position : vec2, size : vec2, rotation : f32 = 0:f32, quad : Quad = (Quad (vec2 0 0) (vec2 1 1)),
                      fliph? : bool = false, flipv? : bool = false, color : vec4 = (vec4 1))

        self.outdated-vertices? = true
        self.outdated-indices? = true

        local norm-vertices =
            # 0 - 1
            # | \ |
            # 2 - 3
            arrayof vec2
                vec2 0 1 # top left
                vec2 1 1 # top right
                vec2 0 0 # bottom left
                vec2 1 0 # bottom right

        local texcoords =
            arrayof vec2
                vec2 0 0 # top left
                vec2 1 0 # top right
                vec2 0 1 # bottom left
                vec2 1 1 # bottom right

        inline make-vertex (vertex-index uv-index)
            vertex :=
                +
                    math.rotate2D
                        (norm-vertices @ vertex-index) - (vec2 0.5)
                        rotation
                    vec2 0.5

            VertexAttributes
                position = (position + (vertex * size))
                texcoords = (quad.start + ((texcoords @ uv-index) * quad.extent))
                color = color

        # position in the batched vertex buffer + current vertex in this batch
        vtx-offset := self.vertex-offset + (countof self.vertex-data)
        vtx-offset as:= u32

        inline get-idx (input)
            input as:= u8
            # fliph makes index switch from odd to even (toggle bit 0)
            # flipv swaps 0,1 <-> 2,3 (toggle bit 1)
            output := bxor input (u8 fliph?)
            output := bxor output ((u8 flipv?) << 1)
            _ input output

        'append self.vertex-data
            make-vertex (get-idx 0)
        'append self.vertex-data
            make-vertex (get-idx 1)
        'append self.vertex-data
            make-vertex (get-idx 2)
        'append self.vertex-data
            make-vertex (get-idx 3)

        'append self.index-data (0:u32 + vtx-offset)
        'append self.index-data (2:u32 + vtx-offset)
        'append self.index-data (3:u32 + vtx-offset)
        'append self.index-data (3:u32 + vtx-offset)
        'append self.index-data (1:u32 + vtx-offset)
        'append self.index-data (0:u32 + vtx-offset)
        ;

    fn... add-polygon (self, center, segments, radius, rotation : f32 = 0:f32, color : vec4 = (vec4 1))
        self.outdated-vertices? = true
        self.outdated-indices? = true

        radius as:= f32
        vtx-offset := self.vertex-offset + (countof self.vertex-data)
        vtx-offset as:= u32

        segments := (max (u32 segments) 3:u32)

        'append self.vertex-data
            VertexAttributes (position = center) (color = color)
        for i in (range (segments + 1))
            angle := (f32 i) * (2 * pi / (f32 segments)) + (pi / 2) + rotation
            'append self.vertex-data
                VertexAttributes
                    position = center + ((math.rotate2D (vec2 1 0) angle) * radius)
                    color = color
        for i in (range (segments + 1))
            'append self.index-data vtx-offset # center
            'append self.index-data (vtx-offset + i)
            'append self.index-data (vtx-offset + i + 1)
        ()

    fn... add-circle (self, center, radius, color : vec4 = (vec4 1), segments : (param? i32) = none)
        self.outdated-vertices? = true
        self.outdated-indices? = true

        let segments =
            static-if (none? segments)
                radius as:= f32
                errorv := 0.33
                segments := math.ceil (pi / (acos (1 - errorv / (max radius errorv))))
                max 5:u32 (u32 segments)
            else
                segments

        'add-polygon self center segments radius 0:f32 color
        ()

    fn finish (self render-pass)
        'flush self render-pass
        self.vertex-offset = 0
        self.index-offset = 0

do
    let GeometryBatch
    local-scope;
