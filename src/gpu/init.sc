using import String
using import struct

import sdl
import .wgpu
import .types

using import ..helpers
using import .common
using import .errors

import ..window

fn create-surface ()
    static-match operating-system
    case 'linux
        let x11-display x11-window = (window.get-native-info)
        wgpu.InstanceCreateSurface istate.instance
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
        wgpu.InstanceCreateSurface istate.instance
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
    istate.swapchain = (create-swapchain (window.get-drawable-size))

fn init ()
    cfg := from (import ..config) let istate-cfg

    istate.instance =
        wgpu.CreateInstance
            &local wgpu.InstanceDescriptor

    istate.surface = (create-surface)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter istate.instance
        &local wgpu.RequestAdapterOptions
            compatibleSurface = istate.surface
            powerPreference = cfg.gpu.power-preference
        fn (status result msg userdata)
            istate.adapter = result
            ;
        null

    wgpu.AdapterRequestDevice istate.adapter
        &local wgpu.DeviceDescriptor
            requiredLimits =
                &local wgpu.RequiredLimits
                    limits =
                        wgpu.Limits
                            maxBindGroups = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureDimension1D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureDimension2D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureDimension3D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureArrayLayers = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxDynamicUniformBuffersPerPipelineLayout = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxDynamicStorageBuffersPerPipelineLayout = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxSampledTexturesPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxSamplersPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxStorageBuffersPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxStorageTexturesPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxUniformBuffersPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxUniformBufferBindingSize = wgpu.WGPU_LIMIT_U64_UNDEFINED
                            maxStorageBufferBindingSize = wgpu.WGPU_LIMIT_U64_UNDEFINED
                            minUniformBufferOffsetAlignment = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            minStorageBufferOffsetAlignment = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxVertexBuffers = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxBufferSize = wgpu.WGPU_LIMIT_U64_UNDEFINED
                            maxVertexAttributes = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxVertexBufferArrayStride = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxInterStageShaderComponents = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupStorageSize = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeInvocationsPerWorkgroup = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupSizeX = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupSizeY = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupSizeZ = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupsPerDimension = wgpu.WGPU_LIMIT_U32_UNDEFINED
        fn (status result msg userdata)
            if (status != wgpu.RequestDeviceStatus.Success)
                print msg
            istate.device = result
            ;
        null

    wgpu.DeviceSetUncapturedErrorCallback istate.device
        fn (errtype message userdata)
            raising noreturn
            print (String message)
            # TODO: rework functionality here to work AOT
            # print
            #     errtype as wgpu.ErrorType
            #     "\n"
            #     # FIXME: replace by something less stupid later; we don't want to use `string` anyway.
            #     loop (result next = str"" (string message))
            #         let match? start end =
            #             try
            #                 ('match? str"\\\\n" next) # for some reason newlines are escaped in shader error messages
            #             else
            #                 _ false 0 0
            #         if match?
            #             _
            #                 .. result (lslice next start) "\n"
            #                 rslice next end
            #         else
            #             break (result .. next)
            ;
        null

    istate.swapchain = (create-swapchain (window.get-drawable-size))
    istate.queue = (wgpu.DeviceGetQueue istate.device)

    # binding-interface.make-dummy-resources istate
    # binding-interface.make-default-pipeline-layouts istate
    ;

fn set-clear-color (color)
    istate.clear-color = color

fn begin-frame ()
    using types

    if (window.minimized?)
        raise GPUError.OutdatedSwapchain

    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        raise GPUError.OutdatedSwapchain

    color-attachment := ColorAttachment (TextureView swapchain-image) istate.clear-color
    RenderPass color-attachment

fn present (render-pass)
    'submit ('finish render-pass)
    wgpu.SwapChainPresent istate.swapchain
    ;

do
    let init update-render-area set-clear-color begin-frame present
    let types
    let get-preferred-surface-format
    import .info

    locals;
