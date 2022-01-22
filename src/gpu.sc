using import struct

using import .helpers

import .window

let sdl = (import .FFI.sdl)
let wgpu = (import .FFI.wgpu)

struct GfxState plain
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue

    # we're probably gonna remove these
    default-pipeline : wgpu.RenderPipeline
    default-bgroup-layout : wgpu.BindGroupLayout
    default-bgroup : wgpu.BindGroup

global istate : GfxState

# MODULE FUNCTIONS START HERE
# ================================================================================
fn create-surface ()
    static-match operating-system
    case 'linux
        let x11-display x11-window = (window.get-native-info)
        wgpu.InstanceCreateSurface null
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromXlib
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromXlib
                            display = (x11-display as voidstar)
                            window = (x11-window as u32)
                        mutable@ wgpu.ChainedStruct
    case 'windows
        let hinstance hwnd = (window.get-native-info)
        wgpu.InstanceCreateSurface null
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromWindowsHWND
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromWindowsHWND
                            hinstance = hinstance
                            hwnd = hwnd
                        mutable@ wgpu.ChainedStruct
    default
        error "OS not supported"

fn create-swapchain (width height)
    wgpu.DeviceCreateSwapChain istate.device istate.surface
        &local wgpu.SwapChainDescriptor
            label = "swapchain"
            usage = wgpu.TextureUsage.RenderAttachment
            format = (wgpu.SurfaceGetPreferredFormat istate.surface istate.adapter)
            width = (width as u32)
            height = (height as u32)
            presentMode = wgpu.PresentMode.Fifo

fn init ()
    istate.surface = (create-surface)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter null
        &local wgpu.RequestAdapterOptions
            compatibleSurface = istate.surface
            powerPreference = wgpu.PowerPreference.HighPerformance
        fn (status result msg userdata)
            istate.adapter = result
            ;
        null

    wgpu.AdapterRequestDevice istate.adapter
        &local wgpu.DeviceDescriptor
            requiredLimits =
                &local wgpu.RequiredLimits
        fn (status result msg userdata)
            istate.device = result
        null
    ;

    istate.swapchain = (create-swapchain (window.get-size))
    ;

do
    let init
    locals;
