using import Array
using import String
using import struct

using import .common
using import ..helpers
using import ..asset.ImageData
using import ..exceptions
using import .texture-format
import .wgpu

type Texture <:: wgpu.Texture
    inline... __typecall (cls,
                          width : u32,
                          height : u32,
                          format : (param? wgpu.TextureFormat) = none,
                          image-data : (param? ImageData) = none,
                          render-target? = false,
                          dimension = wgpu.TextureDimension.2D,
                          slices = 1:u32,
                          mipmap-levels = 1:u32,
                          generate-mipmaps? = false,
                          sample-count = 1:u32)

        usage :=
            | wgpu.TextureUsage.CopyDst
                wgpu.TextureUsage.TextureBinding
                (render-target? wgpu.TextureUsage.RenderAttachment (bitcast 0 wgpu.TextureUsage))

        let format =
            static-if (none? format)
                image-data.format
            else format

        handle :=
            wgpu.DeviceCreateTexture istate.device
                &local wgpu.TextureDescriptor
                    label = "Bottle Texture"
                    usage = usage
                    dimension = dimension
                    size = (wgpu.Extent3D width height slices)
                    format = format
                    mipLevelCount = mipmap-levels
                    sampleCount = sample-count

        self := wrap-nullable-object cls handle

        static-if (not (none? image-data))
            'frame-write (view self) image-data

        self
    case (cls, image-data : ImageData)
        this-function cls
            copy image-data.width
            copy image-data.height
            copy image-data.format
            image-data

    fn get-size (self)
        width := wgpu.TextureGetWidth self
        height := wgpu.TextureGetHeight self
        slices := wgpu.TextureGetDepthOrArrayLayers self
        _ width height slices

    fn get-format (self)
        wgpu.TextureGetFormat self

    fn... frame-write (self, image-data : ImageData, x = 0:u32, y = 0:u32, z = 0:u32, mip-level = 0:u32, aspect = wgpu.TextureAspect.All)
        format := 'get-format self
        if (image-data.format != format)
            raise GPUError.InvalidOperation #S"Mismatched formats between ImageData and Texture"

        block-size := get-texel-block-size format aspect

        iwidth iheight islices := image-data.width, image-data.height, image-data.slices
        twidth theight tslices := 'get-size self
        data-size          := iwidth * iheight * islices * block-size
        texture-size-bytes := twidth * theight * tslices * block-size
        buffer-offset      := x * y * z * block-size

        ptr count := 'data image-data.data
        assert (count == data-size) "malformed ImageData"

        if (data-size > (texture-size-bytes - buffer-offset))
            raise GPUError.InvalidInput #S"Writing image data at offset exceeds texture bounds"

        wgpu.QueueWriteTexture istate.queue
            &local wgpu.ImageCopyTexture
                texture = self
                mipLevel = mip-level
                origin = (wgpu.Origin3D x y z)
                aspect = aspect
            ptr
            data-size
            &local wgpu.TextureDataLayout
                offset = buffer-offset
                bytesPerRow = iwidth * block-size
                rowsPerImage = iheight
            &local wgpu.Extent3D iwidth iheight islices

type TextureView <:: wgpu.TextureView
    inline... __typecall (cls, source-texture : Texture)
        wrap-nullable-object cls
            wgpu.TextureCreateView source-texture null # TODO: allow configuration via descriptor

do
    let TextureView Texture
    local-scope;
