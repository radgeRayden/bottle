using import .common
using import glsl
using import glm
using import struct

inline... 2Drotate (v : vec2, angle : f32)
    let rcos rsin = (cos angle) (sin angle)
    vec2
        (rcos * v.x) - (rsin * v.y)
        (rsin * v.x) + (rcos * v.y)
fn vert ()
    out vcolor : vec4
        location = 0

    struct QuadArray plain
        data : (array Quad)

    buffer input-data : QuadArray readonly
        set = 0
        binding = 0

    uniform uniforms : Uniforms
        set = 0
        binding = 1

    local vertices =
        # 0 - 1
        # 2 - 3
        arrayof vec2
            vec2 -0.5 -0.5
            vec2 0.5 -0.5
            vec2 -0.5 0.5
            vec2 0.5 0.5

    quad := (input-data.data @ gl_InstanceIndex)
    size := quad.size
    time := uniforms.time
    dir := quad.direction

    orientation := quad.rotation * time
    displacement := (vec2 (cos (dir + orientation)) (sin (dir + orientation))) * quad.speed * time

    vertex := ((2Drotate (vertices @ gl_VertexIndex) orientation) * size) + displacement
    gl_Position = uniforms.mvp * (vec4 vertex 0.0 1.0)
    vcolor = quad.color
