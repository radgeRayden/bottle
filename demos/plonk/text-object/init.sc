using import Array enum glm itertools Map Option radl.IO.FileStream String struct print slice
import bottle ...demo-common UTF-8
using bottle.gpu.types

plonk := bottle.plonk

struct FontAtlas
    texture : Texture
    glyphs : (Map i32 plonk.Quad)
    tofu : plonk.Quad

struct ImageFontMetrics
    spacing : f32
    line-height : f32
    y-offset : f32
    scale : f32

struct GlyphDrawInfo plain
    quad : plonk.Quad
    uv : plonk.Quad

enum TextAlignment plain
    Left
    Right
    Center

struct TextObject
    codepoints : (Array i32)
    font-atlas : FontAtlas
    font-metrics : ImageFontMetrics
    wrap : f32
    geometry : (Array GlyphDrawInfo)
    alignment : TextAlignment

    fn... set-text (self, text : String)
        'clear self.codepoints
        ->>
            text
            UTF-8.decoder
            filter ((x) -> (x > 0))
            self.codepoints
        ()
        'update-geometry self

    fn update-geometry (self)
        atlas-size := self.font-atlas.texture.Size
        metrics := self.font-metrics
        local scratch-word : (Array plonk.Quad)
        'reserve scratch-word (countof self.codepoints)
        local pen : vec2
        local word-width : f32
        local line-start : i32

        'clear self.geometry
        for idx c in (enumerate self.codepoints)
            inline end-line ()
                switch self.alignment
                case 'Right
                    last-char := ('last self.geometry) . quad
                    free-space := self.wrap - last-char.start.x - last-char.extent.x
                    for i in (range line-start (countof self.geometry))
                        quad := (self.geometry @ i) . quad
                        quad.start.x += free-space
                case 'Center
                    last-char := ('last self.geometry) . quad
                    free-space := self.wrap - last-char.start.x - last-char.extent.x
                    left-offset := floor (free-space / 2)
                    for i in (range line-start (countof self.geometry))
                        quad := (self.geometry @ i) . quad
                        quad.start.x += left-offset
                default
                    ()
                line-start = i32 (countof self.geometry)

            inline finish-word ()
                if (word-width + pen.x > self.wrap)
                    end-line;
                    pen = vec2 0 (pen.y - metrics.line-height * metrics.scale)
                for g in scratch-word
                    'append self.geometry
                        GlyphDrawInfo
                            quad = plonk.Quad pen ((vec2 atlas-size.xy) * g.extent * metrics.scale)
                            uv = g
                    pen.x += ((g.extent.x * (f32 atlas-size.x)) + metrics.spacing) * metrics.scale
                'clear scratch-word
                word-width = 0

            switch c
            case c"\n"
                finish-word;
                if (idx > 0)
                    end-line;
                pen = vec2 0 (pen.y - metrics.line-height * metrics.scale)
            case c" "
                finish-word;
                glyph := 'getdefault self.font-atlas.glyphs c self.font-atlas.tofu
                character-width := glyph.extent.x * (f32 atlas-size.x)
                pen += vec2 ((character-width + metrics.spacing) * metrics.scale) 0
            case c"\t"
                finish-word;
            default
                glyph := 'getdefault self.font-atlas.glyphs c self.font-atlas.tofu
                character-width := glyph.extent.x * (f32 atlas-size.x)
                'append scratch-word glyph
                word-width += (character-width + metrics.spacing) * metrics.scale

    fn set-wrap (self width)
        width := f32 width
        if (width != self.wrap)
            self.wrap = width
            'update-geometry self

    fn draw (self position)
        for g in self.geometry
            plonk.sprite self.font-atlas.texture (position + g.quad.start) g.quad.extent 0:f32 g.uv (origin = (vec2))

struct DemoContext
    text-object : TextObject
    bg-color : vec4

global ctx : (Option DemoContext)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "TextObject: the text rendering"
    cfg.window.width = 520
    cfg.window.height = 320
    cfg.gpu.present-mode = 'FifoRelaxed

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
                        spacing = -8
                        line-height = 14
                        y-offset = 5
                        scale = 2
                wrap = 400
                alignment = 'Center

        font-string := S"!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_ abcdefghijklmnopqrstuvwxyz(|)~"
        first-cell := 33
        cells-h cells-v := 16, 8
        inline get-quad (cell)
            quad := plonk.Quad
                vec2 ((1 / 16) * (f32 (cell % cells-h))) ((1 / 8) * (f32 (cell // cells-h)))
                vec2 (1 / 16) (1 / 8)
        for i c in (enumerate font-string)
            'set text-object.font-atlas.glyphs (i32 c) (get-quad (first-cell + i))
        text-object.font-atlas.tofu = get-quad (first-cell + 30)

        test-string := try! ('read-all-string (FileStream "assets/example.txt" FileMode.Read))
        'set-text text-object test-string

        ctx =
            DemoContext
                text-object = text-object
    else (assert false)

@@ 'on bottle.update
fn "update" (dt)
    ctx := 'force-unwrap ctx
    ww wh := (bottle.window.get-size)
    'set-wrap ctx.text-object ww

@@ 'on bottle.render
fn ()
    raising bottle.exceptions.GPUError
    ctx := 'force-unwrap ctx
    plonk.set-texture-filtering 'Nearest 'Nearest
    ww wh := (bottle.window.get-size)
    'draw ctx.text-object (vec2 0 (wh - 80))
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
