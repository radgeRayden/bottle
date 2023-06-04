using import enum
using import struct
using import ..helpers
using import .common

import .wgpu
using import .GPUBuffer
using import .Texture
using import .Sampler

type BindGroupLayout <:: wgpu.BindGroupLayout

type BindGroup <:: wgpu.BindGroup
    inline __typecall (cls layout entries...)
        local entries =
            arrayof wgpu.BindGroupEntry
                va-map
                    inline (idx)
                        entry := va@ idx entries...
                        inline... match-entry (entry : GPUBuffer)
                            _
                                buffer = (imply (view entry) wgpu.Buffer)
                                offset = 0
                                size = ('get-byte-size entry) as u64
                        case (entry : TextureView)
                            _
                                textureView = (imply (view entry) wgpu.TextureView)
                        case (entry : Sampler)
                            _
                                sampler = entry

                        wgpu.BindGroupEntry (binding = idx)
                            match-entry entry

                    va-range (va-countof entries...)

        wrap-nullable-object cls
            wgpu.DeviceCreateBindGroup istate.device
                &local wgpu.BindGroupDescriptor
                    label = "bottle bind group"
                    layout = layout
                    entryCount = (countof entries) as u32
                    entries = &entries

do
    let BindGroupLayout BindGroup
    local-scope;
