using import enum
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
    line-index : u32

enum LineJoinKind plain
    Bevel
    Miter
    Round

enum LineCapKind plain
    Butt
    Square
    Round

struct LineUniforms plain
    mvp : mat4
    join-kind = LineJoinKind.Bevel
    cap-kind = LineCapKind.Butt
    semicircle-segments : f32 = 25
    width : f32

struct GenericUniforms plain
    mvp : mat4

struct Quad plain
    start : vec2
    extent : vec2

do
    let Quad VertexAttributes LineSegment LineUniforms GenericUniforms \
        LineJoinKind LineCapKind
    local-scope;
