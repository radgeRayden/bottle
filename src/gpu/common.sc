using import struct

let wgpu = (import ..FFI.wgpu)

struct GfxDummyResources
    buffer : wgpu.Buffer
    sampler : wgpu.Sampler
    texture-view : wgpu.TextureView

struct GfxState
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue

    dummy-resources : GfxDummyResources

global istate : GfxState

do
    let istate
    locals;
