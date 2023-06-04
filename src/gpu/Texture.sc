using import Array
using import struct

using import .common
using import ..helpers
using import ..ImageData
using import .errors
using import .texture-format
import .wgpu

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
                    size = (wgpu.Extent3D width height slices)
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
                texture = self
                mipLevel = mip-level
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
