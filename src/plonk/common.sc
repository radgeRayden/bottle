using import glm
using import struct

struct VertexAttributes plain
    position : vec2
    texcoords : vec2
    color : vec4

struct Uniforms
    mvp : mat4

struct Quad plain
    start : vec2
    extent : vec2

do
    let Quad VertexAttributes Uniforms
    local-scope;
