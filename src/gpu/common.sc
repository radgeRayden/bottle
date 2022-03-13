using import struct
using import Map
using import String

let wgpu = (import ..FFI.wgpu)

struct GfxDummyResources
    buffer : wgpu.Buffer
    sampler : wgpu.Sampler
    texture-view : wgpu.TextureView

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

do
    let istate
    locals;
