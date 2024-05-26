using import Array glm Map Option print radl.ext radl.rect-pack \
    String struct UTF-8

import ..demo-common
import bottle fontdue
using bottle.types

plonk := bottle.plonk

global test-string =
    S""""Crazy? I Was Crazy Once. They Locked Me In A Room.
         A Rubber Room. A Rubber Room With Rats. And Rats Make Me Crazy.

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "font rendering"
    cfg.platform.force-x11? = false
    cfg.gpu.present-mode = 'Mailbox

struct Timer plain
    elapsed-time : f64
    time-limit : f64
    repeat? : bool
    callback : (@ (function void))

    fn update (self dt)
        self.elapsed-time += dt
        if (self.elapsed-time >= self.time-limit)
            self.callback;
            if self.repeat?
                'reset self

    fn reset (self)
        if (self.elapsed-time < self.time-limit)
            self.elapsed-time = 0
            return;

        while (self.elapsed-time >= self.time-limit)
            self.elapsed-time -= self.time-limit

struct DemoContext
    font : FontFamily
    # font-atlas : FontAtlas
    atlas-texture : plonk.TextureBinding
    font-size : f32

    packing-timer : Timer
    glyphs : (Array fontdue.GlyphMapping)
    glyph-map : (Map u32 uvec4)
    atlas : Atlas
    atlas-size : u32

    animation-started? : bool

global ctx : (Option DemoContext)

fn pack-new-glyph ()
    ctx := 'force-unwrap ctx
    if (empty? ctx.glyphs)
        return;
    next-glyph := 'pop ctx.glyphs
    local metrics : fontdue.Metrics
    # we probably don't need to use indexed here since we already have
    # the collection stored. TBD.
    fontdue.font_metrics_indexed ctx.font next-glyph.index ctx.font-size &metrics
    gw gh := metrics.width, metrics.height

    if (gw == 0 or gh == 0)
        return;

    local buf : (Array u8)
    'resize buf (gw * gh)
    ptr count := 'data buf

    fontdue.font_rasterize_indexed ctx.font next-glyph.index ctx.font-size
        typeinit@
            metrics = metrics
            data = dupe ptr
            data_length = count

    local image-data = ImageData (u32 gw) (u32 gh)

    using import itertools
    for x y in (dim gw gh)
        v := buf @ (gw * y + x)
        idx := (gw * y + x) * 4
        image-data.data @ (idx + 0) = v
        image-data.data @ (idx + 1) = v
        image-data.data @ (idx + 2) = v
        image-data.data @ (idx + 3) = v

    local rect = AtlasRect 0 0 (i32 (gw + 2)) (i32 (gh + 2))
    fits? := 'pack ctx.atlas rect

    if (not fits?)
        main-space := (ctx.atlas.spaces @ 0)
        side := i32 (deref ctx.atlas-size)
        scale-factor := 1.5
        ctx.atlas-size = u32 ((f32 ctx.atlas-size) * scale-factor)

        # for now let's assume the first character always fits
        # while (side < gw or side < gh)
        #     side *= 2

        local new-atlas : Atlas
        'clear new-atlas (i32 ((f32 side) * scale-factor))

        local old-atlas = AtlasRect 0 0 side side
        'pack new-atlas old-atlas

        # FIXME: how do we actually handle this if it fails?
        rect = AtlasRect 0 0 (i32 (gw + 2)) (i32 (gh + 2))
        assert
            'pack new-atlas rect

        ctx.atlas = new-atlas
        try!
            do
                new-atlas-texture := Texture ctx.atlas-size ctx.atlas-size (format = 'RGBA8UnormSrgb)
                'copy-texture (bottle.gpu.get-cmd-encoder) \
                    ctx.atlas-texture.texture 0 (uvec3) 'All \
                    new-atlas-texture 0 (uvec3) 'All \
                    ctx.atlas-texture.texture.Size

                ctx.atlas-texture = typeinit new-atlas-texture

    try
        'frame-write ctx.atlas-texture.texture image-data (u32 (rect.x + 1)) (u32 (rect.y + 1))
        'set ctx.glyph-map next-glyph.character (uvec4 (rect.x + 1) (rect.y + 1) rect.w rect.h)
    except (ex)
        print ex
    ()

@@ 'on bottle.load
fn ()
    using bottle.types

    font-data := try!
        'read-all-bytes (FileStream "assets/monogram.ttf" FileMode.Read)

    try
        font-size := 32.0
        font := FontFamily font-data font-size
        char-count := (fontdue.font_char_count font)
        print "chars:" char-count
        print "glyphs:" (fontdue.font_glyph_count font)

        # should be a buffer?
        local glyph-mappings : (Array fontdue.GlyphMapping)
        'resize glyph-mappings char-count
        fontdue.font_chars font ('data glyph-mappings) ()

        local atlas : Atlas
        'clear atlas 128

        ctx =
            DemoContext
                font = font
                # font-atlas
                atlas-texture =
                    typeinit
                        Texture 128 128 (format = 'RGBA8UnormSrgb) # TODO: change to single channel
                packing-timer =
                    Timer (callback = pack-new-glyph) (time-limit = (1 / 60:f64)) (repeat? = true)
                glyphs = glyph-mappings
                font-size = font-size
                atlas = atlas
                atlas-size = 128

        while (not (empty? (('force-unwrap ctx) . glyphs)))
            pack-new-glyph;
    else (abort)

@@ 'on bottle.key-pressed
fn (k)
    if (k == 'Space)
        (('force-unwrap ctx) . animation-started?) ^= true

@@ 'on bottle.update
fn (dt)
    try
        'unwrap ctx
    then (ctx)
        # if ctx.animation-started?
        #     'update ctx.packing-timer dt

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap ctx

    tsize := vec2 ctx.atlas-texture.texture.Size.xy
    # plonk.sprite ctx.atlas-texture (vec2 100) tsize 0:f32
    #     plonk.Quad (vec2) (vec2 1)
    #     origin = (vec2 0)
    # plonk.rectangle-line (vec2 100) tsize 0:f32 (vec2 0) (color = (vec4 1 0 0 1))
    # for s in ctx.atlas.spaces
    #     plonk.rectangle-line ((vec2 100) + (vec2 s.x (tsize.y - (f32 s.y)))) (vec2 s.w s.h) 0:f32 (vec2 0 1) (color = (vec4 0.5 0.5 0.5 0.5))

    layout := fontdue.layout_new 'PositiveYUp
    fontdue.layout_reset layout
        fontdue.LayoutSettings
            x = 50.5
            y = 600.5
            constrain_width = true
            max_width = 300
            horizontal_align = 'Left
            vertical_align = 'Top
            line_height = 1.0
            wrap_style = 'Word
            wrap_hard_breaks = true

    fontdue.layout_append layout ctx.font 1
        typeinit
            text = dupe (test-string as rawstring)
            px = ctx.font-size
            font_index = 0
            null
    glyph-count := fontdue.layout_glyphs_count layout
    local layout-glyphs : (Array fontdue.GlyphPosition)
    'resize layout-glyphs glyph-count
    fontdue.layout_glyphs layout ('data layout-glyphs) ()

    for g in layout-glyphs
        inline get-quad (pxrect)
            pxrect := (vec4 pxrect)
            v0 v1 := pxrect.xy / tsize, pxrect.zw / tsize
            plonk.Quad v0 v1

        # FIXME: not UTF-8 awares
        c := test-string @ g.byte_offset
        let glyph-rect =
            try ('get ctx.glyph-map (c as u32))
            else
                (print c)
                (uvec4)

        plonk.sprite ctx.atlas-texture (vec2 g.x g.y) (vec2 g.width g.height) 0:f32 (get-quad glyph-rect)
            origin = (vec2 0)
        ()
    fontdue.layout_free layout

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
