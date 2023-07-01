using import glm
using import struct

struct VertexAttributes plain
    position : vec2
    texcoords : vec2
    color : vec4

struct LineSegment plain
    color : vec4
    start : vec2
    end   : vec2
    width : f32

struct LineUniforms plain
    mvp : mat4

struct GenericUniforms plain
    mvp : mat4

struct Quad plain
    start : vec2
    extent : vec2

do
    let Quad VertexAttributes LineSegment LineUniforms GenericUniforms
    local-scope;
