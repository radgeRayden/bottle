using import Array
using import struct

from (import .common) let istate
using import ..helpers
using import .errors
import .wgpu

fn... get-texel-block-size (format, aspect : (param? wgpu.TextureAspect) = none)
    using wpgu.TextureFormat
    using wgpu.TextureAspect

    inline match-range (value first last)
        value >= first and value <= last

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
        switch aspect
        case TextureAspect.StencilOnly 1
        default
            raise GPUError.InvalidOperation
    case Depth32FloatStencil8
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

type TextureView <:: wgpu.TextureView

type Texture <:: wgpu.Texture
    inline... __typecall (cls,
                          width : u32,
                          height : u32,
                          format = wgpu.TextureFormat.RGBA8UnormSrgb,
                          data : (param? (Array u8)) = none,
                          render-target? = false,
                          dimension = wgpu.TextureDimension.2D,
                          slices = 1:u32,
                          mipmap-levels = 1:u32,
                          generate-mipmaps? = false)

        usage := wgpu.TextureUsage.CopyDst | wgpu.TextureUsage.TextureBinding | (render-target? wgpu.TextureUsage.RenderAttachment 0)

        handle :=
            wgpu.DeviceCreateTexture istate.device
                &local wgpu.TextureDescriptor
                    label = "Bottle Texture"
                    usage =
                    dimension = dimension
                    size = (wgpu.Extent3D width height layers)
                    format = format
                    mipLevelCount = 1
                    sampleCount = 1

        self := wrap-nullable-object cls handle

        static-if (not (none? data))
            'frame-write self data

        self

    fn get-size (self)
        width := wgpu.TextureGetWidth self
        height := wgpu.TextureGetHeight self
        slices := wgpu.TextureGetDepthOrArrayLayers self
        _ width height slices

    fn get-format (self)
        wgpu.TextureGetFormat self

    fn... frame-write (self, data : (Array u8), x = 0:u32, y = 0:u32, z = 0:u32, mip-level = 0:u32, aspect = wgpu.TextureAspect.All)
        # image data size must not exceed image size - origin
        width height slices := 'get-size self
        block-size := get-texel-block-size aspect ('get-format self)
        ptr count := 'data data

        buffer-size := width * height * block-size
        buffer-offset := x * y * block-size
        if (count > (buffer-size - buffer-offset))
            raise GPUError.InvalidInput

        wgpu.QueueWriteTexture istate.queue
            &local wgpu.ImageCopyTexture
                texture = handle
                mipLevel = mipLevel
                origin = (wgpu.Origin3D x y z)
                aspect = aspect
            ptr
            &local wgpu.TextureDataLayout
                offset = buffer-offset
                bytesPerRow = width * block-size
                rowsPerImage = height
            # FIXME: wrong for data that doesn't cover the whole image. Pending ImageData struct with the info
            &local wgpu.Extent3D width height slices

do
    let TextureView Texture
    local-scope;
