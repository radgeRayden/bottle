using import struct
using import ..helpers
using import .common
using import .errors
using import .render-pass

import .binding-interface
import ..window

import sdl
import wgpu

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
                        &local wgpu.SurfaceDescriptorFromXlibWindow
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromXlibWindow
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
            format = (get-preferred-surface-format)
            width = (width as u32)
            height = (height as u32)
            presentMode = wgpu.PresentMode.Fifo

fn update-render-area ()
    istate.swapchain = (create-swapchain (window.get-size))

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
            raising noreturn
            print
                errtype as wgpu.ErrorType
                "\n"
                # FIXME: replace by something less stupid later; we don't want to use `string` anyway.
                loop (result next = str"" (string message))
                    let match? start end =
                        try
                            ('match? str"\\\\n" next) # for some reason newlines are escaped in shader error messages
                        else
                            _ false 0 0
                    if match?
                        _
                            .. result (lslice next start) "\n"
                            rslice next end
                    else
                        break (result .. next)
            ;
        null

    istate.swapchain = (create-swapchain (window.get-size))
    istate.queue = (wgpu.DeviceGetQueue istate.device)

    binding-interface.make-dummy-resources istate
    binding-interface.make-default-pipeline-layouts istate
    ;

fn begin-frame ()
    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        raise GPUError.OutdatedSwapchain

    let cmd-encoder =
        wgpu.DeviceCreateCommandEncoder istate.device
            (&local wgpu.CommandEncoderDescriptor)

    let render-pass =
        RenderPass cmd-encoder
            arrayof wgpu.RenderPassColorAttachment
                typeinit
                    view = swapchain-image
                    loadOp = wgpu.LoadOp.Clear
                    storeOp = wgpu.StoreOp.Store
                    clearValue = (typeinit 0.017 0.017 0.017 1.0)

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
    let init update-render-area begin-frame present

    vvv bind types
    do
        from (import .pipeline)    let GPUPipeline GPUShaderModule
        from (import .render-pass) let RenderPass
        from (import .buffer)      let GPUBuffer GPUStorageBuffer GPUIndexBuffer GPUUniformBuffer
        # from (import .common)      let GPUResourceBinding
        locals;

    locals;
