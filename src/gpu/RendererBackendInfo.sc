import C.string
using import String
using import struct
using import property

import .wgpu
using import .common
using import radl.strfmt

struct WGPUVersion
    major : u8
    minor : u8
    patch : u8
    revision : u8

    inline __typecall (cls value)
        using import format
        super-type.__typecall cls
            major = ((value >> 24) & 0xFF) as u8
            minor = ((value >> 16) & 0xFF) as u8
            patch = ((value >> 8) & 0xFF) as u8
            revision = (value & 0xFF) as u8

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

        inline make-String (rstr)
            String rstr (C.string.strlen rstr)

        super-type.__typecall cls
            version = typeinit (wgpu.GetVersion)
            vendor = make-String p.vendorName
            architecture = make-String p.architecture
            device = make-String p.name
            driver = make-String p.driverDescription
            adapter = p.adapterType
            low-level-api = p.backendType

do
    let RendererBackendInfo
    local-scope;
