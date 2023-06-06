import .wgpu
using import ..exceptions
using import ..helpers

fn... get-texel-block-size (format : wgpu.TextureFormat, aspect : (param? wgpu.TextureAspect) = none)
    Format := wgpu.TextureFormat
    Aspect := wgpu.TextureAspect

    switch format
    pass Format.R8Unorm
    pass Format.R8Snorm
    pass Format.R8Uint
    pass Format.R8Sint
    do 1:u32
    pass Format.RG8Unorm
    pass Format.RG8Snorm
    pass Format.RG8Uint
    pass Format.RG8Sint
    # pass Format.R16Unorm
    # pass Format.R16Snorm
    pass Format.R16Uint
    pass Format.R16Sint
    pass Format.R16Float
    do 2:u32
    pass Format.RGBA8Unorm
    pass Format.RGBA8UnormSrgb
    pass Format.RGBA8Snorm
    pass Format.RGBA8Uint
    pass Format.RGBA8Sint
    pass Format.BGRA8Unorm
    pass Format.BGRA8UnormSrgb
    # pass Format.RG16Unorm
    # pass Format.RG16Snorm
    pass Format.RG16Uint
    pass Format.RG16Sint
    pass Format.RG16Float
    pass Format.R32Uint
    pass Format.R32Sint
    pass Format.R32Float
    pass Format.RGB9E5Ufloat
    pass Format.RGB10A2Unorm
    # pass Format.Rg11B10Float
    do 4:u32
    # pass Format.RGBA16Unorm
    # pass Format.RGBA16Snorm
    pass Format.RGBA16Uint
    pass Format.RGBA16Sint
    pass Format.RGBA16Float
    pass Format.RG32Uint
    pass Format.RG32Sint
    pass Format.RG32Float
    do 8:u32
    pass Format.RGBA32Uint
    pass Format.RGBA32Sint
    pass Format.RGBA32Float
    do 16:u32
    case Format.Stencil8 1:u32
    case Format.Depth16Unorm 2:u32
    case Format.Depth32Float 4:u32
    case Format.Depth24Plus
        raise GPUError.InvalidOperation
    case Format.Depth24PlusStencil8
        static-if (none? aspect)
            raise GPUError.InvalidOperation
        else
            switch aspect
            case Aspect.StencilOnly 1:u32
            default
                raise GPUError.InvalidOperation
    case Format.Depth32FloatStencil8
        static-if (none? aspect)
            raise GPUError.InvalidOperation
        else
            switch aspect
            case Aspect.DepthOnly 4:u32
            case Aspect.StencilOnly 1:u32
            default
                raise GPUError.InvalidOperation
    pass Format.BC1RGBAUnorm
    pass Format.BC1RGBAUnormSrgb
    pass Format.BC4RUnorm
    pass Format.BC4RSnorm
    do 8:u32
    pass Format.BC2RGBAUnorm
    pass Format.BC2RGBAUnormSrgb
    pass Format.BC3RGBAUnorm
    pass Format.BC3RGBAUnormSrgb
    pass Format.BC5RGUnorm
    pass Format.BC5RGSnorm
    pass Format.BC6HRGBUfloat
    pass Format.BC6HRGBFloat
    pass Format.BC7RGBAUnorm
    pass Format.BC7RGBAUnormSrgb
    do 16:u32
    pass Format.ETC2RGB8Unorm
    pass Format.ETC2RGB8UnormSrgb
    pass Format.ETC2RGB8A1Unorm
    pass Format.ETC2RGB8A1UnormSrgb
    pass Format.EACR11Unorm
    pass Format.EACR11Snorm
    do 8:u32
    pass Format.ETC2RGBA8Unorm
    pass Format.ETC2RGBA8UnormSrgb
    pass Format.EACRG11Unorm
    pass Format.EACRG11Snorm
    do 16:u32
    default
        # match all ASTC formats
        if (format >= Format.ASTC4x4Unorm and format <= Format.ASTC12x12UnormSrgb)
            16:u32
        else
            raise GPUError.InvalidOperation

do
    let get-texel-block-size
    local-scope;
