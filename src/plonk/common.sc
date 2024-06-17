using import enum glm struct ..types

struct VertexAttributes plain
    position : vec2
    texcoords : vec2
    color : vec4

struct LineSegment plain
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

struct LineData plain
    join-kind = LineJoinKind.Bevel
    cap-kind = LineCapKind.Butt
    semicircle-segments : u32 = 25
    width : f32
    color : vec4

struct PlonkTransform plain
    mvp : mat4

struct Quad plain
    start : vec2
    extent : vec2

do
    let PlonkTransform Quad VertexAttributes LineSegment LineData \
        LineJoinKind LineCapKind
    local-scope;
