using import String
using import struct
using import property

import .wgpu
using import ..context radl.strfmt .types

ctx := context-accessor 'gpu

type+ RendererBackendInfo
    BackendString :=
        property
            inline "getter" (self)
                s := self
                f"WebGPU ${s.version} over ${s.low-level-backend}"
    GPUString :=
        property
            inline "getter" (self)
                s := self
                f"${s.device} (${s.adapter}) - driver ${s.driver}"

    fn collect-info (self)
        local p : wgpu.AdapterInfo
        wgpu.AdapterGetInfo ctx.adapter &p

        self =
            this-type
                version = typeinit (wgpu.GetVersion)
                vendor = p.vendor
                architecture = p.architecture
                device = p.device
                driver = p.description
                adapter = p.adapterType
                low-level-backend = p.backendType

()
