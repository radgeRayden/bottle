using import String
using import struct
using import property

import .wgpu
using import .common
using import radl.strfmt

struct WGPUVersion
    major : u32
    minor : u32
    patch : u32
    revision : u32

    inline __typecall (cls value)
        using import format
        super-type.__typecall cls
            major = (value >> 24) & 0xFF
            minor = (value >> 16) & 0xFF
            patch = (value >> 8) & 0xFF
            revision = value & 0xFF

    inline __repr (self)
        f"v${self.major}.${self.minor}.${self.patch}.${self.revision}"

struct RendererBackendInfo
    version : WGPUVersion
    vendor : String
    architecture : String
    device : String
    driver : String
    adapter : wgpu.AdapterType
    low-level-api : wgpu.BackendType

    APIString :=
        property
            inline "getter" (self)
                s := self
                f"WebGPU ${s.version} over ${s.low-level-api}"
    GPUString :=
        property
            inline "getter" (self)
                s := self
                f"${s.device} (${s.adapter}) - driver ${s.driver}"

    inline __typecall (cls)
        local p : wgpu.AdapterProperties
        wgpu.AdapterGetProperties istate.adapter &p

        super-type.__typecall cls
            version = typeinit (wgpu.GetVersion)
            vendor = 'from-rawstring String p.vendorName
            architecture = 'from-rawstring String p.architecture
            device = 'from-rawstring String p.name
            driver = 'from-rawstring String p.driverDescription
            adapter = p.adapterType
            low-level-api = p.backendType

do
    let RendererBackendInfo
    local-scope;
