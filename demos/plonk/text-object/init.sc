using import Array glm itertools Map Option String struct
import bottle ...demo-common UTF-8
plonk := bottle.plonk

struct DemoContext
    font-atlas : plonk.TextureBinding
    character-mappings : (Map i32 plonk.Quad)
    tofu : plonk.Quad
    bg-color : vec4
    test-string = S"Hello World\nHello LÖVE discord server"

    advance : f32
    line-height : f32
    y-offset : f32

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

        local demo-context =
            DemoContext
                font-atlas =
                    plonk.TextureBinding
                        Texture image-font
                        min-filter = 'Nearest
        font-string := S"!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_ abcdefghijklmnopqrstuvwxyz(|)~"

        first-cell := 33
        cells-h cells-v := 16, 8
        inline get-quad (cell)
            quad := plonk.Quad
                vec2 ((1 / 16) * (f32 (cell % cells-h))) ((1 / 8) * (f32 (cell // cells-h)))
                vec2 (1 / 16) (1 / 8)
        for i c in (enumerate font-string)
            'set demo-context.character-mappings (i32 c) (get-quad (first-cell + i))
        demo-context.tofu = get-quad (first-cell + 30)
        demo-context.advance = 14
        demo-context.y-offset = 5
        demo-context.line-height = 24

        ctx = demo-context

    else ()

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap ctx

    let codepoints =
        ->>
            ctx.test-string
            UTF-8.decoder
            filter ((x) -> (x > 0))
            local dst : (Array i32)

    fold (pen = (vec2 0 600)) for c in codepoints
        if (c == c"\n")
            vec2 0 (pen.y - ctx.line-height)
        else
            let quad =
                try
                    'get ctx.character-mappings c
                else
                    deref ctx.tofu

            position := pen + (vec2 0 ctx.y-offset)
            plonk.sprite ctx.font-atlas position (vec2 32 32) 0:f32 quad (origin = (vec2))
            pen + (vec2 ctx.advance 0)

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
