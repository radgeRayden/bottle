using import Array glm Map .math radl.ext radl.rect-pack Rc struct .gpu.types UTF-8
import fontdue

fn rasterize-glyph (buf font ch size)
    local metrics : fontdue.Metrics
    fontdue.font_metrics font ch size &metrics
    gw gh := metrics.width, metrics.height

    'resize buf (gw * gh)
    ptr size := 'data buf

    fontdue.font_rasterize font ch size
        typeinit@
            metrics = metrics
            data = dupe ptr
            data_length = size
    _ gw gh

struct GlyphInfo
    rect : AtlasRect
    metrics : fontdue.Metrics

struct FontAtlas
    atlas  : Texture
    glyphs : (Map u32 GlyphInfo)
    size   : f32

    inline __typecall (cls font size)
        local buf : (Array u8)
        for i in (range (char " ") ((char"~") + 1))
            gw gh := rasterize-glyph buf font i size

typedef FTDFont :: fontdue.Font
    inline __drop (self)
        fontdue.font_free self

    inline __imply (thisT otherT)
        static-if (otherT == fontdue.Font)
            inline (self)
                bitcast self fontdue.Font

typedef FontFamily <:: (Rc FTDFont)
    DefaultSize := 16.0
    DefaultBias := 1.0

    inline... __typecall (cls, font-data, sizes, quality-bias : f32)
        vvv bind smallest largest
        static-if (imply? sizes Generator)
            fold (smallest largest = 0.0 0.0) for s in sizes
                _ (min smallest s) (max largest s)
        else
            _ sizes sizes

        t := clamp quality-bias 0.0 1.0
        font-scale := ceil (fmix smallest largest t)

        ptr size := 'data font-data
        font :=
            fontdue.font_new_from_bytes ptr size
                typeinit
                    collection_index = 0
                    scale = font-scale
        bitcast (Rc.wrap font) this-type
    case (cls, font-data, sizes)
        this-function cls font-data sizes DefaultBias
    case (cls, font-data)
        local sizes = (arrayof f32 DefaultSize)
        this-function cls font-data sizes DefaultBias

    fn rasterize-characters (self)

    fn rasterize-all-characters (self)

    fn rasterize-range (self)

    inline __drop ()

do
    let FontFamily
    local-scope;
