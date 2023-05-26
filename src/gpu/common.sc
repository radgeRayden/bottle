using import struct
using import Map
using import String
using import glm

import wgpu
# from (import .binding-interface) let GPUResourceBinding

# struct GfxDummyResources
#     buffer : GPUResourceBinding
#     uniform-buffer : GPUResourceBinding
#     sampler : GPUResourceBinding
#     texture-view : GPUResourceBinding

# struct GfxCachedLayouts
#     bind-group-layouts : (Map String wgpu.BindGroupLayout)
#     pipeline-layouts : (Map String wgpu.PipelineLayout)

struct GfxState
    instance  : wgpu.Instance
    surface   : wgpu.Surface
    adapter   : wgpu.Adapter
    device    : wgpu.Device
    swapchain : wgpu.SwapChain
    queue     : wgpu.Queue

    # dummy-resources : GfxDummyResources
    # cached-layouts  : GfxCachedLayouts

    clear-color = (vec4 0.017 0.017 0.017 1.0)

global istate : GfxState

fn get-preferred-surface-format ()
    local capabilities : wgpu.SurfaceCapabilities
    wgpu.SurfaceGetCapabilities istate.surface istate.adapter &capabilities

    let selected-format = wgpu.TextureFormat.BGRA8UnormSrgb
        # FIXME: currently the surface format array is not getting filled, so we can't iterate over it
        # fold (selected-format = wgpu.TextureFormat.Undefined) for i in (range capabilities.formatCount)
        #     let format = (capabilities.formats @ i)
        #     if (format == wgpu.TextureFormat.BGRA8UnormSrgb)
        #         break (copy format)
        #     else
        #         selected-format

    assert (selected-format != wgpu.TextureFormat.Undefined)
        "it was not possible to select an appropriate surface format on this platform"

    selected-format

do
    let istate get-preferred-surface-format
    locals;
