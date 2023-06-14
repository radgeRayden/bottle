using import glm
using import Option
using import struct
using import ..gpu.types
using import ..types
using import .common

struct SpriteAtlas
    texture : Texture
    texture-view : TextureView
    bind-group : (Option BindGroup)
    columns : i32
    rows : i32

    inline... __typecall (cls, image-data : ImageData, columns = 1, rows = 1)
        texture := Texture image-data
        texture-view := TextureView texture

        super-type.__typecall cls
            texture = texture
            texture-view = texture-view
            columns = columns
            rows = rows

    fn... get-quad (self, frame, fliph? = false, flipv? = false)
        rows columns := self.rows, self.columns
        frame-count := rows * columns
        idx := frame % frame-count
        let frame =
            if (idx < 0) (frame-count - idx)
            else idx

        x y := frame % columns, frame // columns
        w h := 1 / columns, 1 / rows
        x y := (f32 x) * w, (f32 y) * h

        let x w =
            if fliph? (_ (x + w) -w)
            else (_ x w)
        let y h =
            if flipv? (_ (y + h) -h)
            else (_ y h)

        Quad
            start = vec2 x y
            extent = vec2 w h

do
    let SpriteAtlas
    local-scope;
