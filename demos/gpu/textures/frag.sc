using import glsl
using import glm

fn frag ()
    uniform t : texture2D (set = 1) (binding = 1)
    uniform s : sampler (set = 1) (binding = 0)

    in uv : vec2 (location = 0)
    out fcolor : vec4 (location = 0)

    fcolor = texture (sampler2D t s) uv
