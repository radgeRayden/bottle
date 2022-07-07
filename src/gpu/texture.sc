import wgpu
using import struct

from (import .common) let istate
using import ..helpers

struct GPUTexture
    _handle : wgpu.Texture
    _view : wgpu.TextureView

    inline __typecall (cls data width height)
        let handle =
            wgpu.DeviceCreateTexture istate.device
                &local wgpu.TextureDescriptor
                    label = "Bottle Texture"
                    usage = (wgpu.TextureUsage.CopyDst | wgpu.TextureUsage.TextureBinding)
                    dimension = wgpu.TextureDimension.2D
                    size = (wgpu.Extent3D (width as u32) (height as u32) 1)
                    format = wgpu.TextureFormat.RGBA8UnormSrgb
                    mipLevelCount = 1
                    sampleCount = 1

        wgpu.QueueWriteTexture istate.queue
            &local wgpu.ImageCopyTexture
                texture = handle
                mipLevel = 0
                origin = (wgpu.Origin3D)
                aspect = wgpu.TextureAspect.All
            data as voidstar
            width * height * 4
            &local wgpu.TextureDataLayout
                offset = 0
                bytesPerRow = ((width as u32) * (sizeof f32))
                rowsPerImage = (height as u32)
            &local wgpu.Extent3D (width as u32) (height as u32) 1

        let texture-view =
            wgpu.TextureCreateView handle
                &local wgpu.TextureViewDescriptor
                    format = wgpu.TextureFormat.RGBA8UnormSrgb
                    dimension = wgpu.TextureViewDimension.2D
                    baseMipLevel = 0
                    mipLevelCount = 1
                    baseArrayLayer = 0
                    arrayLayerCount = 1
                    aspect = wgpu.TextureAspect.All

        super-type.__typecall cls
            _handle = handle
            _view = texture-view

    inline __drop (self)
        wgpu.TextureDrop self._handle
        wgpu.TextureViewDrop self._view
        ;

do
    let GPUTexture
    locals;
