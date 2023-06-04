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
        .. (va-map tostring 'v self.major '. self.minor '. self.patch '. self.revision)

struct RendererBackendInfo
    version : WGPUVersion
    vendor : String
    architecture : String
    device : String
    driver : String
    adapter : wgpu.AdapterType
    low-level-api : wgpu.BackendType

    RendererString :=
        property
            inline "getter" (self)
                s := self
                str :=
                    f""""WebGPU ${s.version} over ${s.low-level-api}
                         ${s.device} (${s.adapter}) - driver ${s.driver}
                lslice str ((countof str) - 1) # remove newline

    inline __typecall (cls)
        local p : wgpu.AdapterProperties
        wgpu.AdapterGetProperties istate.adapter &p

        super-type.__typecall cls
            version = typeinit (wgpu.GetVersion)
            vendor = String p.vendorName
            architecture = String p.architecture
            device = String p.name
            driver = String p.driverDescription
            adapter = p.adapterType
            low-level-api = p.backendType

do
    let RendererBackendInfo
    local-scope;
