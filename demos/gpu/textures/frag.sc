using import glsl
using import glm

fn frag ()
    uniform s : sampler (set = 0) (binding = 0)
    uniform t : texture2D (set = 0) (binding = 1)

    in uv : vec2 (location = 0)
    out fcolor : vec4 (location = 0)

    fcolor = texture (sampler2D t s) uv
