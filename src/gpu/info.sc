using import String
using import struct

import .wgpu
using import .common

fn get-vendor (id)
    switch id
    case 0x10de
        S"NVIDIA Corporation"
    case 0x1002
        S"Advanced Micro Devices, Inc. [AMD/ATI]"
    case 0x8086
        S"Intel Corporation"
    default
        S"Unknown Vendor"

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

struct GPUBackendInfo
    wgpu-version : WGPUVersion
    vendor : String
    _wgpu-adapter-properties : wgpu.AdapterProperties

fn get-backend-info ()
    local properties : wgpu.AdapterProperties
    wgpu.AdapterGetProperties istate.adapter &properties

    local info : GPUBackendInfo
        _wgpu-adapter-properties = properties
        vendor = get-vendor properties.vendorID
        wgpu-version = typeinit (wgpu.GetVersion)

    info

do
    let get-backend-info
        GPUBackendInfo
    locals;
