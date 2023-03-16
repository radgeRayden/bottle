import wgpu
using import struct

from (import .common) let istate
using import ..helpers

struct GPUTexture
    _handle : wgpu.Texture
    _view : wgpu.TextureView
    _width : u32
    _height : u32

    inline __typecall (cls data width height ...)
        let format = (va-option format ... wgpu.TextureFormat.RGBA8UnormSrgb)
        let channels = (va-option channels ... 4)

        let handle =
            wgpu.DeviceCreateTexture istate.device
                &local wgpu.TextureDescriptor
                    label = "Bottle Texture"
                    usage = (wgpu.TextureUsage.CopyDst | wgpu.TextureUsage.TextureBinding)
                    dimension = wgpu.TextureDimension.2D
                    size = (wgpu.Extent3D (width as u32) (height as u32) 1)
                    format = format
                    mipLevelCount = 1
                    sampleCount = 1

        static-if (not (none? data))
            wgpu.QueueWriteTexture istate.queue
                &local wgpu.ImageCopyTexture
                    texture = handle
                    mipLevel = 0
                    origin = (wgpu.Origin3D)
                    aspect = wgpu.TextureAspect.All
                data as voidstar
                width * height * channels
                &local wgpu.TextureDataLayout
                    offset = 0
                    bytesPerRow = ((width as u32) * channels)
                    rowsPerImage = (height as u32)
                &local wgpu.Extent3D (width as u32) (height as u32) 1

        let texture-view =
            wgpu.TextureCreateView handle
                &local wgpu.TextureViewDescriptor
                    format = format
                    dimension = wgpu.TextureViewDimension.2D
                    baseMipLevel = 0
                    mipLevelCount = 1
                    baseArrayLayer = 0
                    arrayLayerCount = 1
                    aspect = wgpu.TextureAspect.All

        super-type.__typecall cls
            _handle = handle
            _view = texture-view
            _width = width
            _height = height

    fn write (self data)
        let width height = self._width self._height
        wgpu.QueueWriteTexture istate.queue
            &local wgpu.ImageCopyTexture
                texture = self._handle
                mipLevel = 0
                origin = (wgpu.Origin3D)
                aspect = wgpu.TextureAspect.All
            (imply data pointer) as voidstar
            width * height * 4
            &local wgpu.TextureDataLayout
                offset = 0
                bytesPerRow = ((width as u32) * (sizeof f32))
                rowsPerImage = (height as u32)
            &local wgpu.Extent3D (width as u32) (height as u32) 1
        ;

    inline __drop (self)
        wgpu.TextureDrop self._handle
        wgpu.TextureViewDrop self._view
        ;

do
    let GPUTexture
    locals;
