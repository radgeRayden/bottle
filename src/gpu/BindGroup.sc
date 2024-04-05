using import Array enum struct
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

    struct BindGroupBuilder
        entries : (Array wgpu.BindGroupEntry)
        layout : BindGroupLayout

        fn... add-entry (self, entry : GPUBuffer, offset : u64 = 0:u64)
            'append self.entries
                wgpu.BindGroupEntry (binding = ((countof self.entries) as u32))
                    buffer = (imply (view entry) wgpu.Buffer)
                    offset = offset
                    size = ('get-byte-size entry) as u64
        case (self, entry : TextureView)
            'append self.entries
                wgpu.BindGroupEntry (binding = ((countof self.entries) as u32))
                    textureView = view entry
        case (self, entry : Sampler)
            'append self.entries
                wgpu.BindGroupEntry (binding = ((countof self.entries) as u32))
                    sampler = view entry

        fn... set-layout (self, layout : wgpu.BindGroupLayout)
            self.layout = copy layout

        fn finalize (self)
            using import .common

            ptr count := 'data self.entries
            wrap-nullable-object BindGroup
                wgpu.DeviceCreateBindGroup ctx.device
                    typeinit@
                        label = "bottle bind group"
                        layout = self.layout
                        entryCount = count
                        entries = dupe ptr

    inline builder (cls)
        (BindGroupBuilder)

()
