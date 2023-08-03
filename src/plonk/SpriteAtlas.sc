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

    fn... get-quad (self, frame : i32)
        rows columns := self.rows, self.columns
        frame-count := rows * columns
        idx := frame % frame-count
        let frame =
            if (idx < 0) (frame-count - idx)
            else idx

        x y := frame % columns, frame // columns
        w h := 1 / columns, 1 / rows
        x y := (f32 x) * w, (f32 y) * h

        Quad
            start = vec2 x y
            extent = vec2 w h
    case (self, column : i32, row : i32)
        Quad
            start = vec2 (column / self.columns, row / self.rows)
            extent = vec2 (1 / self.columns, 1 / self.rows)

    fn get-quad-size (self)
        tw th := 'get-size self.texture
        vec2
            (1 / self.columns) * (tw as f32)
            (1 / self.rows) * (th as f32)
do
    let SpriteAtlas
    local-scope;
