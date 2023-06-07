using import glm
using import struct

struct VertexAttributes plain
    position : vec2
    texcoords : vec2
    color : vec4

struct Uniforms
    mvp : mat4

do
    let VertexAttributes Uniforms
    local-scope;
