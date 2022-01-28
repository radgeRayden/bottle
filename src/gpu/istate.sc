using import enum
using import struct

let wgpu = (import ..FFI.wgpu)

enum GPUResourceBindingType
    Buffer :
        buffer = wgpu.Buffer
        offset = u64
        size = u64
    Sampler : wgpu.Sampler
    TextureView : wgpu.TextureView

    fn make-wgpu-descriptor (self)
        dispatch self
        case Buffer (handle offset size)
            wgpu.BindGroupEntry
                buffer = handle
                offset = offset
                size = size
        case Sampler (handle)
            wgpu.BindGroupEntry
                sampler = handle
        case TextureView (handle)
            wgpu.BindGroupEntry
                textureView = handle
        default
            unreachable;

    inline __hash (self)
        dispatch self
        case Buffer (handle offset size)
            hash handle offset size
        case Sampler (handle)
            hash handle
        case TextureView (handle)
            hash handle
        default
            unreachable;

struct GfxState
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue

global istate : GfxState

do
    let istate
    locals;
