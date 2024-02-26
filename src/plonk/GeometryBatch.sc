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

fn calculate-circle-segment-count (radius)
    radius as:= f32
    errorv := 0.33
    segments := math.ceil (pi / (acos (1 - errorv / (max radius errorv))))
    max 5:u32 (u32 segments)

fn regular-polygon-point (center radius idx segments rotation-offset)
    angle := (f32 idx) * (2 * pi / (f32 segments)) + (pi / 2) + rotation-offset
    center + ((vec2 (cos angle) (sin angle)) * radius)

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
                        module = vert
                        entry-point = "main"
                fragment-stage =
                    FragmentStage
                        module = frag
                        entry-point = "main"
                        color-targets =
                            typeinit
                                ColorTarget
                                    format = TextureFormat.BGRA8UnormSrgb
                msaa-samples = (gpu.msaa-enabled?) 4:u32 1:u32

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

    fn begin-frame (self)
        ()

    fn flush (self render-pass)
        if (not (self.outdated-vertices? or self.outdated-indices?))
            return;

        # NOTE: 'frame-write happens at the very beginning of the command buffer, which means
        # we must avoid copying over what has been written. That's why now when resizing I pass in how much was
        # previously written into the buffer prior to cloning.
        if self.outdated-vertices?
            attrbuf := self.attribute-buffer
            try
                'frame-write attrbuf self.vertex-data self.vertex-offset
            else
                # resize then try again
                self.attribute-buffer =
                    'clone attrbuf (max (countof self.vertex-data) (attrbuf.Capacity * 2:usize)) self.vertex-offset
                return (this-function self render-pass)

            if (self.cached-buffer-id != ('get-id attrbuf))
                self.bind-group =
                    BindGroup ('get-bind-group-layout self.pipeline 0) self.uniform-buffer attrbuf
                self.cached-buffer-id = ('get-id attrbuf)

            self.outdated-vertices? = false

        if self.outdated-indices?
            idxbuf := self.index-buffer
            try
                'frame-write idxbuf self.index-data self.index-offset
            else
                # resize then try again
                self.index-buffer =
                    'clone idxbuf (max (countof self.index-data) (idxbuf.Capacity * 2:usize)) self.vertex-offset
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
                    origin : vec2 = (vec2 0.5), fliph? : bool = false, flipv? : bool = false, color : vec4 = (vec4 1))

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
            v := ((norm-vertices @ vertex-index) - origin) * size
            VertexAttributes
                position = (position + (math.rotate2D v rotation))
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
            'append self.vertex-data
                VertexAttributes
                    position = (regular-polygon-point center radius i segments rotation)
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
            static-if (none? segments) (calculate-circle-segment-count radius)
            else segments

        'add-polygon self center segments radius 0:f32 color
        ()

    fn... add-line (self, vertices, width : f32 = 1.0, color : vec4 = (vec4 1),
                    join-kind : LineJoinKind = LineJoinKind.Bevel,
                    cap-kind : LineCapKind = LineCapKind.Butt)
        self.outdated-vertices? = true
        self.outdated-indices? = true

        if ((countof vertices) < 2)
            return;

        local segment-vertices =
            arrayof vec2
                vec2 (-0.5,  1.0) #tl
                vec2 ( 0.5,  1.0) #tr
                vec2 (-0.5,  0.0) #bl
                vec2 ( 0.5,  0.0) #br

        inline next-index ()
            u32 (self.vertex-offset + (countof self.vertex-data))

        for i in (range ((countof vertices) - 1))
            start end := vertices @ i, vertices @ (i + 1)
            inline make-vertex (idx)
                dir := end - start
                perp := normalize (vec2 dir.y -dir.x)

                vertex := segment-vertices @ idx
                vpos := start + (dir * vertex.y) + (perp * vertex.x * width)
                VertexAttributes
                    position = vpos
                    color = color

            first-index := (next-index)
            idx := (i) -> ((u32 i) + first-index)
            add-idx := (i) -> ('append self.index-data (idx i))
            add-vtx := (v) -> ('append self.vertex-data v)

            va-map
                inline (i)
                    add-vtx (make-vertex i)
                _ 0 1 2 3

            va-map add-idx
                _ 0 2 3 3 1 0

        join-range := (range 1 ((countof vertices) - 1))
        switch join-kind
        case LineJoinKind.Bevel
            for i in join-range
                a-start ljoin b-end := vertices @ (i - 1), vertices @ i, vertices @ (i + 1)
                first-index := (next-index)
                idx := (i) -> ((u32 i) + first-index)
                add-idx := (i) -> ('append self.index-data (idx i))
                add-vtx := (v) -> ('append self.vertex-data v)

                inline get-perpendicular (start end)
                    dir := end - start
                    normalize (vec2 dir.y -dir.x)

                perp-A perp-B := get-perpendicular a-start ljoin, get-perpendicular ljoin b-end
                tangent := perp-B - perp-A
                normal := normalize (vec2 tangent.y -tangent.x)
                sigma := sign (dot (perp-A + perp-B) normal)

                va-map
                    inline (vpos)
                        add-vtx
                            VertexAttributes
                                position = vpos
                                color = color
                    _
                        ljoin + (perp-A * (width / 2) * sigma)
                        ljoin
                        ljoin + (perp-B * (width / 2) * sigma)
                va-map add-idx
                    _ 0 1 2
        case LineJoinKind.Miter
        case LineJoinKind.Round
            for i in join-range
                # TODO: change to semicircle when that is a thing
                'add-circle self (vertices @ i) (width / 2) color
        default ()

        start end := vertices @ 0, vertices @ ((countof vertices) - 1)
        switch cap-kind
        case LineCapKind.Square
            radius := (width / 2) * (static-eval (sqrt 2:f32))
            start+1 end-1 := vertices @ 1, vertices @ ((countof vertices) - 2)
            sangle eangle := atan2 (unpack ((start+1 - start) . yx)), atan2 (unpack ((end - end-1) . yx))
            'add-polygon self start 4 radius (sangle - (pi / 4)) color
            'add-polygon self end 4 radius (eangle - (pi / 4)) color
        case LineCapKind.Round
            # TODO: change to semicircle when that is a thing
            'add-circle self start (width / 2) color
            'add-circle self end (width / 2) color
        default ()

    fn... add-rectangle-line (self : this-type, position : vec2, size : vec2,
                              rotation : f32 = 0:f32, origin : vec2 = (vec2 0.5), line-width : f32 = 1:f32, color : vec4 = (vec4 1),
                              join-kind : LineJoinKind = LineJoinKind.Bevel,
                              cap-kind : LineCapKind = LineCapKind.Butt)
        self.outdated-vertices? = true
        self.outdated-indices? = true

        local vertices : (array vec2 5)
            va-map
                inline (v)
                    position + (math.rotate2D ((v - origin) * size) rotation)
                _
                    vec2 0 1 # top left
                    vec2 1 1 # top right
                    vec2 1 0 # bottom right
                    vec2 0 0 # bottom left
                    vec2 0 1 # top left
        'add-line self vertices line-width color join-kind cap-kind
        ()

    fn... add-polygon-line (self, center, segments, radius, rotation,
                            line-width : f32 = 1:f32, color : vec4 = (vec4 1),
                            join-kind : LineJoinKind = LineJoinKind.Bevel,
                            cap-kind : LineCapKind = LineCapKind.Butt)
        self.outdated-vertices? = true
        self.outdated-indices? = true

        radius as:= f32
        segments := (max (u32 segments) 3:u32)
        # FIXME: maybe we can avoid this allocation in the future
        local line-vertices : (Array vec2)
        for i in (range (segments + 1))
            'append line-vertices (regular-polygon-point center radius i segments rotation)
        'add-line self line-vertices line-width color join-kind cap-kind
        ()

    fn... add-circle-line (self, center, radius, color : vec4 = (vec4 1),
                           segments : (param? i32) = none, line-width : f32 = 1:f32,
                           join-kind : LineJoinKind = LineJoinKind.Bevel,
                           cap-kind : LineCapKind = LineCapKind.Butt)
        self.outdated-vertices? = true
        self.outdated-indices? = true

        let segments =
            static-if (none? segments) (calculate-circle-segment-count radius)
            else segments

        'add-polygon-line self center segments radius 0:f32 line-width color join-kind cap-kind
        ()

    fn finish (self render-pass)
        'flush self render-pass
        self.vertex-offset = 0
        self.index-offset = 0
        try ('frame-write self.uniform-buffer self.uniforms)
        else ()

do
    let GeometryBatch
    local-scope;
