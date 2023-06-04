using import .common
fn main ()
    using import glsl
    using import glm

    uniform uniforms : Uniforms
        set = 0
        binding = 1

    in vcolor : vec4
        location = 0
    out fcolor : vec4
        location = 0
    fcolor = vcolor
