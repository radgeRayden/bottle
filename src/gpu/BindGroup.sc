using import enum struct
using import ..context ..helpers .types

import .wgpu
from wgpu let typeinit@ chained@

ctx := context-accessor 'gpu

type+ BindGroup
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
                                textureView = view entry
                        case (entry : Sampler)
                            _
                                sampler = view entry

                        wgpu.BindGroupEntry (binding = idx)
                            match-entry entry

                    va-range (va-countof entries...)

        using import .common
        wrap-nullable-object cls
            wgpu.DeviceCreateBindGroup ctx.device
                &local wgpu.BindGroupDescriptor
                    label = "bottle bind group"
                    layout = layout
                    entryCount = (countof entries) as u32
                    entries = &entries

()
