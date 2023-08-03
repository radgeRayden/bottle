using import Buffer
using import slice
using import struct
using import ..enums
using import ..helpers
using import ..gpu.texture-format
wgpu := import ..gpu.wgpu

struct ImageData
    BufferType := (Buffer (mutable@ u8))

    width : u32
    height : u32
    slices : u32
    data : (Slice BufferType)
    format : TextureFormat

    inline... __typecall (cls, width : u32, height : u32, slices : u32 = 1:u32,
                          data : (param? Buffer) = none, format : TextureFormat = TextureFormat.RGBA8UnormSrgb)
        let block-size =
            try (get-texel-block-size format)
            else 4:u32 # I'm not sure this is a good idea.

        expected-size := width * height * slices * block-size
        let data =
            static-if (none? data)
                heapbuffer u8 expected-size
            else
                data

        # TODO: raise an error instead of asserting
        assert ((countof data) == expected-size)
        super-type.__typecall cls width height slices data format
    # TODO: FileData case

do
    let ImageData
    local-scope;
