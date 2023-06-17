using import struct
using import Array
using import UTF-8

import fontdue
using import .asset.ImageData
using import .enums

let ceil =
    (extern 'llvm.ceil.f32 (function f32 f32))

struct Font
    font : fontdue.Font
    font-size : f32
    line-metrics : fontdue.LineMetrics
    char-width : u32

    inline... __typecall (cls data font-size)
        let font =
            fontdue.font_new_from_bytes
                (imply data pointer) as (mutable@ u8)
                countof data
                fontdue.FontSettings
                    collection_index = 0
                    scale = font-size

        local lm : fontdue.LineMetrics
        fontdue.font_horizontal_line_metrics font font-size &lm

        local metrics : fontdue.Metrics
        fontdue.font_metrics font (char32 " ") font-size &metrics

        super-type.__typecall cls
            font         = font
            font-size    = font-size
            line-metrics = lm
            char-width   = ((ceil metrics.advance_width) as u32)

    fn glyph-metrics (self c)
        c as:= u32

        local metrics : fontdue.Metrics
        fontdue.font_metrics self.font c self.font-size &metrics
        metrics

    fn rasterize-glyph (self c buffer)
        c as:= u32

        local metrics : fontdue.Metrics
        fontdue.font_metrics self.font c self.font-size &metrics

        len := metrics.width * metrics.height
        'resize buffer len

        local bitmap =
            fontdue.GlyphBitmap
                metrics = metrics
                data = ((imply buffer pointer) as (mutable@ u8))
                data_length = len

        fontdue.font_rasterize self.font c self.font-size &bitmap

        metrics

    inline rasterize-glyph-range (self first-glyph last-glyph dst-buffer packf userdata)
        local char-buf : (Array u8)
        for idx c in (enumerate (range first-glyph (last-glyph + 1)))
            let metrics = ('rasterize-glyph self (c as u32) char-buf)
            packf (view char-buf) (view dst-buffer) idx metrics userdata

    inline rasterize-all-glyphs (self buffer packf userdata)
        # ...

    fn pack-atlas (self first-glyph last-glyph)
        struct PackedFontAtlasUserData plain
            glyph-width : u32
            glyph-height : u32
            atlas-width : u32
            atlas-height : u32

        fn pack-glyph (char-buf dst-buf idx metrics userdata)
            userdata.atlas-height += userdata.glyph-height
            'append-each dst-buf char-buf
            offset := userdata.atlas-height * userdata.glyph-width
            'resize dst-buf offset

        local atlas-data : (Array u8)
        local userdata : PackedFontAtlasUserData
            self.char-width
            u32 (ceil self.line-metrics.new_line_size)
            self.char-width

        'rasterize-glyph-range self first-glyph last-glyph atlas-data pack-glyph userdata
        ImageData userdata.atlas-width userdata.atlas-height 1 (data = atlas-data) (format = TextureFormat.R8Unorm)

    inline __drop (self)
        fontdue.font_free self.font

do
    let Font
    locals;
