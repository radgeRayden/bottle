using import glm
using import Map
using import Option
using import print
using import String
using import struct
using import ..context ..exceptions

ctx := context-accessor 'gpu

import .wgpu
import .types

# TODO: fill the capability struct in as part of initialization and then have different functions querying this info
fn get-preferred-surface-format ()
    local capabilities : wgpu.SurfaceCapabilities
    wgpu.SurfaceGetCapabilities ctx.surface ctx.adapter &capabilities
    capabilities.formats = alloca-array wgpu.TextureFormat capabilities.formatCount
    capabilities.presentModes = alloca-array wgpu.PresentMode capabilities.presentModeCount
    capabilities.alphaModes = alloca-array wgpu.CompositeAlphaMode capabilities.alphaModeCount

    wgpu.SurfaceGetCapabilities ctx.surface ctx.adapter &capabilities

    let selected-format =
        fold (selected-format = wgpu.TextureFormat.Undefined) for i in (range capabilities.formatCount)
            let format = (capabilities.formats @ i)
            if (format == wgpu.TextureFormat.BGRA8UnormSrgb)
                break (copy format)
            else
                selected-format

    assert (selected-format != wgpu.TextureFormat.Undefined)
        "it was not possible to select an appropriate surface format on this platform"

    selected-format

spice wrap-nullable-object (cls object)
    spice-quote
        if (object == null)
            print "OBJECT CREATION FAILED:" [((tostring ('anchor object)) as string)]
            raise GPUError.ObjectCreationFailed
        else
            imply object cls

do
    let get-preferred-surface-format wrap-nullable-object
    locals;
