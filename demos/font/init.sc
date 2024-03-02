using import Array Map Option print radl.ext radl.rect-pack \
    String struct UTF-8

import ..demo-common
import bottle fontdue

test-string :=
    """"Crazy? I Was Crazy Once. They Locked Me In A Room.
        A Rubber Room. A Rubber Room With Rats. And Rats Make Me Crazy.

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "font rendering"

struct GlyphInfo
    rect : AtlasRect
    metrics : fontdue.Metrics

struct RasterizedFont
    atlas : bottle.types.TextureView
    glyphs : (Map u32 GlyphInfo)

struct DemoContext
    font : RasterizedFont
global ctx : (Option DemoContext)

fn load-font (font-data font-size)
    using bottle.types

    local font-atlas : Atlas
    # TODO: estimate atlas size, then correct if too small
    atlas-size := 60
    'clear font-atlas atlas-size
    local image-data : ImageData atlas-size atlas-size
    local glyphs : (Map u32 GlyphInfo)

    ptr size := 'data font-data
    font :=
        fontdue.font_new_from_bytes ptr size
            typeinit
                collection_index = 0
                scale = font-size

    characters := S"abcdefghijklmnopqrstuvwxyz"
    local bitmap-data : (Array u8)
    for ch in characters
        ch as:= u32
        local metrics : fontdue.Metrics
        fontdue.font_metrics font ch font-size &metrics
        gw gh := metrics.width, metrics.height

        local rect : AtlasRect 0 0 (i32 (gw + 2)) (i32 (gh + 2))
        fits? := 'pack font-atlas rect

        'resize bitmap-data (gw * gh)
        ptr size := 'data bitmap-data
        fontdue.font_rasterize font ch font-size
            typeinit@
                metrics = metrics
                data = dupe ptr
                data_length = size

        using import itertools
        glyph-offset := atlas-size * (rect.y + 1) + (rect.x + 1)
        for x y in (dim gw gh)
            v := bitmap-data @ (gw * y + x)
            idx := (glyph-offset + atlas-size * y + x) * 4
            image-data.data @ (idx + 0) = v
            image-data.data @ (idx + 1) = v
            image-data.data @ (idx + 2) = v
            image-data.data @ (idx + 3) = v

        'set glyphs ch (GlyphInfo rect metrics)
    RasterizedFont (TextureView (Texture image-data)) glyphs

@@ 'on bottle.load
fn ()
    using bottle.types
    try
        try
            'read-all-bytes (FileStream "assets/monogram.ttf" FileMode.Read)
        then (font-data)
            ctx = DemoContext (load-font font-data 16.0)
            ()
        except (ex)
            abort;
    else ()

@@ 'on bottle.update
fn (dt)

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
