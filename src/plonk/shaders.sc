using import .common
using import glm
using import glsl
using import print
using import struct
import ..math

@@ memo
inline make-buffer-type (T max-size...)
    struct ((static-tostring T) .. "Buffer") plain
        data : (array T max-size...)

inline vertex-shader (uniformT bindings...)
    inline (f)
        fn ()
            uniform uniforms : uniformT
                set = 0
                binding = 0

            data-bindings... :=
                va-map
                    inline (i)
                        binding := i + 1
                        buffer input-data : (make-buffer-type (va@ i bindings...))
                            set = 0
                            binding = binding
                        input-data.data
                    va-range (va-countof bindings...)

            out vtexcoords : vec2 (location = 0)
            out vcolor : vec4 (location = 1)

            let position texcoords color = (f uniforms data-bindings...)
            gl_Position = position
            vtexcoords = texcoords
            vcolor = color

do
    @@ vertex-shader PlonkUniforms VertexAttributes
    inline generic-vert (uniforms data)
        idx := gl_VertexIndex
        vertex := data @ idx

        _
            position = uniforms.mvp * (vec4 vertex.position 0.0 1.0)
            texcoords = vertex.texcoords
            color = vertex.color

    @@ vertex-shader PlonkUniforms LineSegment LineData
    inline line-vert (uniforms segments lines)
        local vertices =
            arrayof vec2
                vec2 (-0.5,  1.0) #tl
                vec2 (-0.5,  0.0) #bl
                vec2 ( 0.5,  0.0) #br
                vec2 ( 0.5,  0.0) #br
                vec2 ( 0.5,  1.0) #tr
                vec2 (-0.5,  1.0) #tl

        vertex := vertices @ gl_VertexIndex
        segment := segments @ gl_InstanceIndex
        line := lines @ segment.line-index

        dir := segment.end - segment.start
        perp := normalize (vec2 dir.y -dir.x)
        vpos := segment.start + (dir * vertex.y) + (perp * vertex.x * line.width)

        _
            position = uniforms.mvp * (vec4 vpos 0.0 1.0)
            vtexcoords = (vec2)
            vcolor = line.color

    @@ vertex-shader PlonkUniforms LineSegment LineData
    inline join-vert (uniforms segments lines)
        idx := gl_InstanceIndex
        last next := segments @ idx, segments @ (idx + 1)
        line := lines @ last.line-index

        inline get-perpendicular (segment)
            dir := segment.end - segment.start
            normalize (vec2 dir.y -dir.x)
        perp-A perp-B := get-perpendicular last, get-perpendicular next
        tangent := perp-B - perp-A
        normal := normalize (vec2 tangent.y -tangent.x)

        sigma := sign (dot (perp-A + perp-B) normal)

        let vpos =
            switch line.join-kind
            case LineJoinKind.Bevel
                local vertices =
                    arrayof vec2
                        last.end + (perp-A * (line.width / 2) * sigma)
                        last.end
                        next.start + (perp-B * (line.width / 2) * sigma)

                deref (vertices @ gl_VertexIndex)
            case LineJoinKind.Miter
                (vec2) #TODO
            case LineJoinKind.Round
                radius := line.width / 2
                idx := gl_VertexIndex
                tri := idx // 3
                inline get-segment-angle (i)
                    (pi / (f32 line.semicircle-segments)) * (f32 i)

                local tri-verts =
                    arrayof vec2
                        last.end
                        last.end + (math.rotate2D perp-A (get-segment-angle tri)) * radius
                        last.end + (math.rotate2D perp-A (get-segment-angle (tri + 1))) * radius
                deref (tri-verts @ (idx % 3))
            default (vec2)

        _
            position = uniforms.mvp * (vec4 vpos 0 1)
            texcoords = (vec2)
            color = line.color

    fn generic-frag ()
        uniform s : sampler (set = 1) (binding = 0)
        uniform t : texture2D (set = 1) (binding = 1)

        in vtexcoords : vec2 (location = 0)
        in vcolor : vec4 (location = 1)
        out fcolor : vec4 (location = 0)

        fcolor = (texture (sampler2D t s) vtexcoords) * vcolor

    local-scope;
