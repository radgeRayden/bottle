using import struct
using import ..helpers
using import .common
using import .errors
using import .render-pass

import .binding-interface
import ..window

let sdl = (import ..FFI.sdl)
let wgpu = (import ..FFI.wgpu)

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
            ;
        null

    wgpu.DeviceSetUncapturedErrorCallback istate.device
        fn (errtype message userdata)
            print
                errtype as wgpu.ErrorType
                string message
        null

    istate.swapchain = (create-swapchain (window.get-size))
    istate.queue = (wgpu.DeviceGetQueue istate.device)

    binding-interface.make-dummy-resources istate
    binding-interface.make-default-pipeline-layouts istate
    ;

fn begin-frame ()
    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        istate.swapchain = (create-swapchain (window.get-size))
        raise GPUError.OutdatedSwapchain

    let cmd-encoder =
        wgpu.DeviceCreateCommandEncoder istate.device
            (&local wgpu.CommandEncoderDescriptor)

    let render-pass =
        RenderPass cmd-encoder
            arrayof wgpu.RenderPassColorAttachment
                typeinit
                    view = swapchain-image
                    clearColor = (typeinit 0.017 0.017 0.017 1.0)

    _ render-pass

fn present (render-pass)
    'finish render-pass

    local cmd-buf =
        wgpu.CommandEncoderFinish render-pass._cmd-encoder
            (&local wgpu.CommandBufferDescriptor)

    wgpu.QueueSubmit istate.queue 1 &cmd-buf
    wgpu.SwapChainPresent istate.swapchain
    ;

do
    let init begin-frame present

    vvv bind types
    do
        from (import .pipeline)    let GPUPipeline GPUShaderModule
        from (import .render-pass) let RenderPass
        from (import .buffer)      let GPUBuffer
        # from (import .common)      let GPUResourceBinding
        locals;

    locals;
