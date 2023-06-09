using import .common
using import glm
using import glsl
using import struct

do
    fn default-vert ()
        buffer input-data :
            struct VertexData plain
                data : (array VertexAttributes)
            set = 0
            binding = 0

        uniform uniforms : Uniforms
            set = 1
            binding = 0

        out vtexcoords : vec2 (location = 0)
        out vcolor : vec4 (location = 1)

        idx := gl_VertexIndex
        vertex := (input-data.data @ idx)

        gl_Position = uniforms.mvp * (vec4 vertex.position 0.0 1.0)
        vtexcoords = vertex.texcoords
        vcolor = vertex.color

    fn default-frag ()
        uniform s : sampler (set = 1) (binding = 1)
        uniform t : texture2D (set = 1) (binding = 2)

        in vtexcoords : vec2 (location = 0)
        in vcolor : vec4 (location = 1)
        out fcolor : vec4 (location = 0)

        fcolor = (texture (sampler2D t s) vtexcoords) * vcolor

    fn display-vert ()
        out vtexcoords : vec2 (location = 0)
        out vcolor : vec4 (location = 1)

        local vertices =
            arrayof vec3
                vec3 (-1.0,  1.0, 0.0) #tl
                vec3 (-1.0, -1.0, 0.0) #bl
                vec3 ( 1.0, -1.0, 0.0) #br
                vec3 ( 1.0, -1.0, 0.0) #br
                vec3 ( 1.0,  1.0, 0.0) #tr
                vec3 (-1.0,  1.0, 0.0) #tl

        local vertex-coords =
            arrayof vec2
                vec2 (0.0, 0.0)
                vec2 (0.0, 1.0)
                vec2 (1.0, 1.0)
                vec2 (1.0, 1.0)
                vec2 (1.0, 0.0)
                vec2 (0.0, 0.0)

        idx := gl_VertexIndex
        gl_Position = vec4 (vertices @ idx) 1
        vtexcoords = vertex-coords @ idx
        vcolor = (vec4 1)

    fn display-frag ()
        uniform s : sampler (set = 0) (binding = 0)
        uniform t : texture2D (set = 0) (binding = 1)

        in vtexcoords : vec2 (location = 0)
        in vcolor : vec4 (location = 1)
        out fcolor : vec4 (location = 0)

        fcolor = (texture (sampler2D t s) vtexcoords) * vcolor

    local-scope;
