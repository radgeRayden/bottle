using import Array
using import struct
using import .enums
using import .helpers
using import .gpu.texture-format
wgpu := import .gpu.wgpu

struct ImageData
    width : u32
    height : u32
    slices : u32
    data : (Array u8)
    format : TextureFormat

    inline... __typecall (cls, width : u32, height : u32, slices = 1:u32, data : (param? (Array u8)) = none, format = TextureFormat.RGBA8UnormSrgb)
        expected-size := width * height * slices * (get-texel-block-size format)
        let data =
            static-if (none? data)
                local data = ((Array u8))
                'resize data expected-size
            else
                data

        # TODO: raise an error instead of asserting
        assert ((countof data) == expected-size)
        super-type.__typecall cls width height slices data format
    # TODO: FileData case

do
    let ImageData
    local-scope;
