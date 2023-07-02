using import .common
using import glm
using import glsl
using import struct
import ..math

@@ memo
inline make-buffer-type (T max-size...)
    struct ((tostring T) .. "Buffer") plain
        data : (array T max-size...)

inline vertex-shader (attrT uniformT)
    inline (f)
        fn ()
            buffer input-data : (make-buffer-type attrT)
                set = 0
                binding = 0

            uniform uniforms : uniformT
                set = 0
                binding = 1

            out vtexcoords : vec2 (location = 0)
            out vcolor : vec4 (location = 1)

            f input-data.data uniforms vtexcoords vcolor

do
    @@ vertex-shader VertexAttributes GenericUniforms
    inline generic-vert (data uniforms vtexcoords vcolor)
        idx := gl_VertexIndex
        vertex := data @ idx

        gl_Position = uniforms.mvp * (vec4 vertex.position 0.0 1.0)
        vtexcoords = vertex.texcoords
        vcolor = vertex.color

    @@ vertex-shader LineSegment (make-buffer-type LineUniforms 256)
    inline line-vert (data uniforms vtexcoords vcolor)
        local vertices =
            arrayof vec2
                vec2 (-0.5,  1.0) #tl
                vec2 (-0.5,  0.0) #bl
                vec2 ( 0.5,  0.0) #br
                vec2 ( 0.5,  0.0) #br
                vec2 ( 0.5,  1.0) #tr
                vec2 (-0.5,  1.0) #tl

        vertex := vertices @ gl_VertexIndex
        segment := data @ gl_InstanceIndex
        uniforms := uniforms.data @ segment.line-index


        dir := segment.end - segment.start
        perp := normalize (vec2 dir.y -dir.x)
        vpos := segment.start + (dir * vertex.y) + (perp * vertex.x * uniforms.width)

        gl_Position = uniforms.mvp * (vec4 vpos 0.0 1.0)
        vtexcoords = (vec2)
        vcolor = segment.color

    @@ vertex-shader LineSegment (make-buffer-type LineUniforms 256)
    inline join-vert (data uniforms vtexcoords vcolor)
        idx := gl_InstanceIndex
        last next := data @ idx, data @ (idx + 1)
        uniforms := uniforms.data @ last.line-index

        inline get-perpendicular (segment)
            dir := segment.end - segment.start
            normalize (vec2 dir.y -dir.x)
        perp-A perp-B := get-perpendicular last, get-perpendicular next
        tangent := perp-B - perp-A
        normal := normalize (vec2 tangent.y -tangent.x)

        sigma := sign (dot (perp-A + perp-B) normal)

        let vpos =
            switch uniforms.join-kind
            case LineJoinKind.Bevel
                local vertices =
                    arrayof vec2
                        last.end + (perp-A * (uniforms.width / 2) * sigma)
                        last.end
                        next.start + (perp-B * (uniforms.width / 2) * sigma)

                deref (vertices @ gl_VertexIndex)
            case LineJoinKind.Miter
                (vec2)
            case LineJoinKind.Round
                radius := uniforms.width / 2
                idx := gl_VertexIndex
                tri := idx // 3
                inline get-segment-angle (i)
                    (pi / uniforms.semicircle-segments) * (f32 i)

                local tri-verts =
                    arrayof vec2
                        last.end
                        last.end + (math.rotate2D perp-A (get-segment-angle tri)) * radius
                        last.end + (math.rotate2D perp-A (get-segment-angle (tri + 1))) * radius
                deref (tri-verts @ (idx % 3))
            default (vec2)

        gl_Position = uniforms.mvp * (vec4 vpos 0 1)
        vtexcoords = (vec2)
        vcolor = last.color

    fn generic-frag ()
        uniform s : sampler (set = 1) (binding = 0)
        uniform t : texture2D (set = 1) (binding = 1)

        in vtexcoords : vec2 (location = 0)
        in vcolor : vec4 (location = 1)
        out fcolor : vec4 (location = 0)

        fcolor = (texture (sampler2D t s) vtexcoords) * vcolor

    local-scope;
