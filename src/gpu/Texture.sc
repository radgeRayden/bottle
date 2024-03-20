using import Array glm property String struct

using import .common ..context ..helpers ..asset.ImageData ..exceptions \
    .texture-format .types radl.strfmt

import .wgpu
from wgpu let typeinit@ chained@

ctx := context-accessor 'gpu

type+ Texture
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
            wgpu.DeviceCreateTexture ctx.device
                &local wgpu.TextureDescriptor
                    label = dupe (f"Bottle Texture ${format}" as rawstring)
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

    fn... frame-write (self, image-data : ImageData, x = 0:u32, y = 0:u32, z = 0:u32, mip-level = 0:u32, aspect = wgpu.TextureAspect.All)
        format := self.Format
        if (image-data.format != format)
            raise GPUError.InvalidOperation #S"Mismatched formats between ImageData and Texture"

        block-size := get-texel-block-size format aspect

        iwidth iheight islices := image-data.width, image-data.height, image-data.slices
        twidth theight tslices := unpack self.Size
        data-size          := iwidth * iheight * islices * block-size
        texture-size-bytes := twidth * theight * tslices * block-size
        buffer-offset      := x * y * z * block-size

        ptr count := 'data image-data.data
        assert (count == data-size) "malformed ImageData"

        if (data-size > (texture-size-bytes - buffer-offset))
            raise GPUError.InvalidInput #S"Writing image data at offset exceeds texture bounds"

        wgpu.QueueWriteTexture ctx.queue
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

    let Size =
        property
            inline (self)
                width := wgpu.TextureGetWidth self
                height := wgpu.TextureGetHeight self
                slices := wgpu.TextureGetDepthOrArrayLayers self
                uvec3 width height slices

    let Dimension =
        property
            (self) -> (wgpu.TextureGetDimension self)

    let Format =
        property
            (self) -> (wgpu.TextureGetFormat self)

    let MipLevelCount =
        property
            (self) -> (wgpu.TextureGetMipLevelCount self)

    let SampleCount =
        property
            (self) -> (wgpu.TextureGetSampleCount self)

type+ TextureView
    inline... __typecall (cls, source-texture : Texture)
        wrap-nullable-object cls
            wgpu.TextureCreateView source-texture null # TODO: allow configuration via descriptor

do
    let TextureView Texture
    local-scope;
