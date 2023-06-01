using import glm
using import struct

struct Quad plain
    size      : f32
    rotation  : f32
    speed     : f32
    direction : f32
    color     : vec4

struct Uniforms plain
    mvp  : mat4
    time : f32

do
    let Quad Uniforms
    local-scope;
