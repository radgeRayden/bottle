using import Array glm itertools Map Option String struct
import bottle ...demo-common UTF-8
using bottle.gpu.types

plonk := bottle.plonk

test-string := "Hello World\nHello LÖVE discord server"

struct FontAtlas
    texture : Texture
    character-mappings : (Map i32 plonk.Quad)
    tofu : plonk.Quad

struct ImageFontMetrics
    advance : f32
    line-height : f32
    y-offset : f32

struct TextObject
    codepoints : (Array i32)
    font-atlas : FontAtlas
    font-metrics : ImageFontMetrics

    fn... set-text (self, text : String)
        ->>
            text
            UTF-8.decoder
            filter ((x) -> (x > 0))
            self.codepoints
        ()

    fn draw (self position max-width)
        metrics := self.font-metrics
        atlas := self.font-atlas

        fold (pen = position) for c in self.codepoints
            if (c == c"\n")
                vec2 0 (pen.y - metrics.line-height)
            else
                let quad =
                    try
                        'get atlas.character-mappings c
                    else
                        deref atlas.tofu

                position := pen + (vec2 0 metrics.y-offset)
                plonk.sprite atlas.texture position (vec2 32 32) 0:f32 quad (origin = (vec2))
                pen + (vec2 metrics.advance 0)

struct DemoContext
    text-object : TextObject
    bg-color : vec4

global ctx : (Option DemoContext)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "TextObject: the text rendering"
    cfg.window.width = 520
    cfg.window.height = 320

@@ 'on bottle.load
fn ()
    using bottle.types

    try
        image-font := bottle.asset.load-image "assets/gelatin_mono.png"

        # remove black pixels
        pixel-count := image-font.width * image-font.height
        for i in (range pixel-count)
            d := image-font.data
            i := i * 4
            color := uvec3 (d @ i) (d @ (i + 1)) (d @  (i + 2))

            if (color == (uvec3 0))
                d @ (i + 3) = 0


        local text-object =
            TextObject
                font-atlas = typeinit (Texture image-font)
                font-metrics =
                    ImageFontMetrics
                        advance = 14
                        line-height = 24
                        y-offset = 5

        font-string := S"!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_ abcdefghijklmnopqrstuvwxyz(|)~"
        first-cell := 33
        cells-h cells-v := 16, 8
        inline get-quad (cell)
            quad := plonk.Quad
                vec2 ((1 / 16) * (f32 (cell % cells-h))) ((1 / 8) * (f32 (cell // cells-h)))
                vec2 (1 / 16) (1 / 8)
        for i c in (enumerate font-string)
            'set text-object.font-atlas.character-mappings (i32 c) (get-quad (first-cell + i))
        text-object.font-atlas.tofu = get-quad (first-cell + 30)

        'set-text text-object test-string

        ctx =
            DemoContext
                text-object = text-object

    else ()

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap ctx
    plonk.set-texture-filtering 'Nearest 'Nearest
    'draw ctx.text-object (vec2 0 600) 1000

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
