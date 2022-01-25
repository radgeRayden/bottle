using import struct
let wgpu = (import ..FFI.wgpu)

struct GfxState plain
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue

    # we're probably gonna remove these
    current-render-pass : wgpu.RenderPassEncoder
    default-pipeline : wgpu.RenderPipeline
    default-bgroup-layout : wgpu.BindGroupLayout
    default-bgroup : wgpu.BindGroup

global istate : GfxState

do
    let istate
    locals;
