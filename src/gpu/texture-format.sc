import .wgpu
using import .errors
using import ..helpers

fn... get-texel-block-size (format : wgpu.TextureFormat, aspect : (param? wgpu.TextureAspect) = none)
    using wgpu.TextureFormat
    using wgpu.TextureAspect

    switch format
    pass R8Unorm
    pass R8Snorm
    pass R8Uint
    pass R8Sint
    do 1
    pass RG8Unorm
    pass RG8Snorm
    pass RG8Uint
    pass RG8Sint
    pass R16Unorm
    pass R16Snorm
    pass R16Uint
    pass R16Sint
    pass R16Float
    do 2
    pass RGBA8Unorm
    pass RGBA8UnormSrgb
    pass RGBA8Snorm
    pass RGBA8Uint
    pass RGBA8Sint
    pass BGRA8Unorm
    pass BGRA8UnormSrgb
    pass RG16Unorm
    pass RG16Snorm
    pass RG16Uint
    pass RG16Sint
    pass RG16Float
    pass R32Uint
    pass R32Sint
    pass R32Float
    pass RGB9E5Ufloat
    pass RGB10A2Unorm
    pass Rg11B10Float
    do 4
    pass RGBA16Unorm
    pass RGBA16Snorm
    pass RGBA16Uint
    pass RGBA16Sint
    pass RGBA16Float
    pass RG32Uint
    pass RG32Sint
    pass RG32Float
    do 8
    pass RGBA32Uint
    pass RGBA32Sint
    pass RGBA32Float
    do 16
    case Stencil8 1
    case Depth16Unorm 2
    case Depth32Float 4
    case Depth24Plus
        raise GPUError.InvalidOperation
    case Depth24PlusStencil8
        static-if (none? aspect)
            raise GPUError.InvalidOperation
        else
            switch aspect
            case TextureAspect.StencilOnly 1
            default
                raise GPUError.InvalidOperation
    case Depth32FloatStencil8
        static-if (none? aspect)
            raise GPUError.InvalidOperation
        else
            switch aspect
            case TextureAspect.DepthOnly 4
            case TextureAspect.StencilOnly 1
            default
                raise GPUError.InvalidOperation
    pass BC1RGBAUnorm
    pass BC1RGBAUnormSrgb
    pass BC4RUnorm
    pass BC4RSnorm
    do 8
    pass BC2RGBAUnorm
    pass BC2RGBAUnormSrgb
    pass BC3RGBAUnorm
    pass BC3RGBAUnormSrgb
    pass BC5RGUnorm
    pass BC5RGSnorm
    pass BC6HRGBUfloat
    pass BC6HRGBFloat
    pass BC7RGBAUnorm
    pass BC7RGBAUnormSrgb
    do 16
    pass ETC2RGB8Unorm
    pass ETC2RGB8UnormSrgb
    pass ETC2RGB8A1Unorm
    pass ETC2RGB8A1UnormSrgb
    pass EACR11Unorm
    pass EACR11Snorm
    do 8
    pass ETC2RGBA8Unorm
    pass ETC2RGBA8UnormSrgb
    pass EACRG11Unorm
    pass EACRG11Snorm
    do 16
    default
        # match all ASTC formats
        if (format >= ASTC4x4Unorm and format <= ASTC12x12UnormSrgb)
            16
        else
            raise GPUError.InvalidOperation

do
    let get-texel-block-size
    local-scope;
