using import glsl
using import glm

fn vert ()
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

    out uv : vec2 (location = 0)

    uv = vertex-coords @ gl_VertexIndex
    gl_Position = vec4 (vertices @ gl_VertexIndex) 1.0
    ;
