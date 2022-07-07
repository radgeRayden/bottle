using import struct
using import Map
using import String

import wgpu
from (import .binding-interface) let GPUResourceBinding

struct GfxDummyResources
    buffer : GPUResourceBinding
    uniform-buffer : GPUResourceBinding
    sampler : GPUResourceBinding
    texture-view : GPUResourceBinding

struct GfxCachedLayouts
    bind-group-layouts : (Map String wgpu.BindGroupLayout)
    pipeline-layouts : (Map String wgpu.PipelineLayout)

struct GfxState
    surface   : wgpu.Surface
    adapter   : wgpu.Adapter
    device    : wgpu.Device
    swapchain : wgpu.SwapChain
    queue     : wgpu.Queue

    dummy-resources : GfxDummyResources
    cached-layouts  : GfxCachedLayouts

global istate : GfxState

fn get-preferred-surface-format ()
    local count : u64
    let supported-formats = (wgpu.SurfaceGetSupportedFormats istate.surface istate.adapter &count)

    let selected-format =
        fold (selected-format = wgpu.TextureFormat.Undefined) for i in (range count)
            let format = (supported-formats @ i)
            if (format == wgpu.TextureFormat.BGRA8UnormSrgb)
                break (copy format)
            else
                selected-format

    assert (selected-format != wgpu.TextureFormat.Undefined)
        "it was not possible to select an appropriate surface format on this platform"

    wgpu.Free supported-formats ((sizeof wgpu.TextureFormat) * count) (alignof wgpu.TextureFormat)
    selected-format

do
    let istate get-preferred-surface-format
    locals;
