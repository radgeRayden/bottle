using import glm Map Option String struct UTF-8
import bottle ...demo-common
plonk := bottle.plonk

struct DemoContext
    font-atlas : plonk.TextureBinding
    character-mappings : (Map i32 plonk.Quad)
    tofu : plonk.Quad
    bg-color : vec4
    test-string = S"Hello World"

global ctx : (Option DemoContext)


@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "TextObject: the text rendering"

@@ 'on bottle.load
fn ()
    using bottle.types

    try
        local demo-context =
            DemoContext
                font-atlas =
                    plonk.TextureBinding
                        Texture (bottle.asset.load-image "assets/gelatin_mono.png")
                        min-filter = 'Nearest
        font-string := S"!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_ abcdefghijklmnopqrstuvwxyz(|)~"

        first-cell := 33
        cells-h cells-v := 16, 8
        for i c in (enumerate font-string)
            cell := first-cell + i
            quad := plonk.Quad
                vec2 (f32 ((cell % cells-h) * 16)) (f32 ((cell // cells-v) * 16))
                vec2 16 16
            'set demo-context.character-mappings (i32 c) quad

        ctx = demo-context

    else ()

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap ctx
    fold (pen = (vec2 0 600)) for c in ctx.test-string
        let quad =
            try
                'get ctx.character-mappings (i32 c) # FIXME: hello UTF-8??
            else
                deref ctx.tofu

        plonk.sprite ctx.font-atlas pen (vec2 16 16) 0:f32 quad (origin = (vec2))
        pen + (vec2 16 0)


sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
