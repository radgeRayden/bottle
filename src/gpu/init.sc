using import struct
using import String
using import ..helpers
using import ..logger
using import .common
using import .errors
# using import .render-pass

import .binding-interface
import ..window

import sdl
import .wgpu

# MODULE FUNCTIONS START HERE
# ================================================================================
struct GPUBackendInfo
    gpu : String
    backend : String

fn get-backend-info ()
    local properties : wgpu.AdapterProperties
    wgpu.AdapterGetProperties istate.adapter &properties

    local info : GPUBackendInfo
    let strlen = (extern 'strlen (function usize (@ char)))

    if (properties.name != null)
        info.gpu = (String properties.name (strlen properties.name))
    info.backend =
        do
            switch properties.backendType
            case wgpu.BackendType.Null
                S"Null"
            case wgpu.BackendType.WebGPU
                S"WebGPU"
            case wgpu.BackendType.D3D11
                S"D3D11"
            case wgpu.BackendType.D3D12
                S"D3D12"
            case wgpu.BackendType.Metal
                S"Metal"
            case wgpu.BackendType.Vulkan
                S"Vulkan"
            case wgpu.BackendType.OpenGL
                S"OpenGL"
            case wgpu.BackendType.OpenGLES
                S"OpenGLES"
            default
                S"Unknown"

    info

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
    istate.instance =
        wgpu.CreateInstance
            &local wgpu.InstanceDescriptor

    istate.surface = (create-surface)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter istate.instance
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
            print message
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
    if (window.minimized?)
        raise GPUError.OutdatedSwapchain

    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        raise GPUError.OutdatedSwapchain

    let cmd-encoder =
        wgpu.DeviceCreateCommandEncoder istate.device
            (&local wgpu.CommandEncoderDescriptor)

    local color-attachments =
        arrayof wgpu.RenderPassColorAttachment
            typeinit
                view = swapchain-image
                loadOp = wgpu.LoadOp.Clear
                storeOp = wgpu.StoreOp.Store
                clearValue = (typeinit (unpack istate.clear-color))

    let render-pass =
        wgpu.CommandEncoderBeginRenderPass cmd-encoder
            &local wgpu.RenderPassDescriptor
                label = "Bottle Render Pass"
                colorAttachmentCount = (countof color-attachments)
                colorAttachments =
                    &local color-attachments

    _ render-pass cmd-encoder

fn present (render-pass cmd-encoder)
    wgpu.RenderPassEncoderEnd render-pass

    local cmd-buf =
        wgpu.CommandEncoderFinish cmd-encoder
            (&local wgpu.CommandBufferDescriptor)

    wgpu.QueueSubmit istate.queue 1 &cmd-buf
    wgpu.SwapChainPresent istate.swapchain
    ;

do
    let init update-render-area set-clear-color begin-frame present
    let get-backend-info

    vvv bind types
    do
        from (import .pipeline)    let GPUPipeline GPUShaderModule
        from (import .render-pass) let RenderPass
        from (import .buffer)      let GPUBuffer GPUStorageBuffer GPUIndexBuffer GPUUniformBuffer
        from (import .texture)     let GPUTexture
        # from (import .common)      let GPUResourceBinding
        locals;

    locals;
