using import .common
using import glm
using import glsl
using import struct

do
    fn sprite-vert ()
        buffer input-data :
            struct VertexData plain
                data : (array VertexAttributes)
            set = 0
            binding = 0

        uniform uniforms : Uniforms
            set = 0
            binding = 1

        out vtexcoords : vec2 (location = 0)
        out vcolor : vec4 (location = 1)

        idx := gl_VertexIndex
        vertex := (input-data.data @ idx)

        gl_Position = uniforms.mvp * (vec4 vertex.position 0.0 1.0)
        vtexcoords = vertex.texcoords
        vcolor = vertex.color

    fn line-vert ()
        buffer input-data :
            struct SegmentData plain
                data : (array LineSegment)
            set = 0
            binding = 0

        uniform uniforms : Uniforms
            set = 0
            binding = 1

        out vtexcoords : vec2 (location = 0)
        out vcolor : vec4 (location = 1)

        local vertices =
            arrayof vec2
                vec2 (-0.5,  1.0) #tl
                vec2 (-0.5,  0.0) #bl
                vec2 ( 0.5,  0.0) #br
                vec2 ( 0.5,  0.0) #br
                vec2 ( 0.5,  1.0) #tr
                vec2 (-0.5,  1.0) #tl

        vertex := vertices @ gl_VertexIndex
        segment := input-data.data @ gl_InstanceIndex

        dir := segment.end - segment.start
        perp := normalize (vec2 dir.y -dir.x)
        vpos := segment.start + (dir * vertex.y) + (perp * vertex.x * segment.width)

        gl_Position = uniforms.mvp * (vec4 vpos 0.0 1.0)
        vtexcoords = (vec2)
        vcolor = segment.color

    fn sprite-frag ()
        uniform s : sampler (set = 1) (binding = 0)
        uniform t : texture2D (set = 1) (binding = 1)

        in vtexcoords : vec2 (location = 0)
        in vcolor : vec4 (location = 1)
        out fcolor : vec4 (location = 0)

        fcolor = (texture (sampler2D t s) vtexcoords) * vcolor

    local-scope;
