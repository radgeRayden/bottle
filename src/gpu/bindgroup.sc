using import struct

import wgpu
import .common
using import ..helpers

struct GPUBindGroup
    _handle : wgpu.BindGroup

    inline __typecall (cls layout bindings...)
        local entries =
            arrayof wgpu.BindGroupEntry
                va-map
                    inline (i)
                        let b = (va@ i bindings...)
                        local desc = ('make-wgpu-descriptor b)
                        desc.binding = i
                        desc
                    va-range (va-countof bindings...)

        let istate = common.istate
        let handle =
            wgpu.DeviceCreateBindGroup istate.device
                &local wgpu.BindGroupDescriptor
                    layout = layout
                    entryCount = (countof entries)
                    entries = &entries

        super-type.__typecall cls
            _handle = handle

do
    let GPUBindGroup
    locals;
