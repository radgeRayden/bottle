import C.stdlib
using import print
using import String
using import struct

import sdl
import .wgpu
import .types

# imports necessary to augment all types with their implementation
import .BindGroup .CommandEncoder .GPUBuffer .RenderPass .RenderPipeline .Sampler .ShaderModule .Texture

using import .common
using import ..config
using import ..exceptions
using import ..helpers
using import .RendererBackendInfo

cfg := cfg-accessor 'gpu

import ..window

fn get-info ()
    RendererBackendInfo;

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

fn create-swapchain-resolve-source (width height)
    using types
    try
        TextureView
            Texture (u32 width) (u32 height) (get-preferred-surface-format) none
                render-target? = true
                sample-count = cfg.msaa-samples
    # FIXME: better way of handling or reporting this kind of error?
    else (assert false "FATAL ERROR: could not create MSAA resolve source")

fn msaa-enabled? ()
    cfg.msaa-samples > 1

fn update-render-area ()
    istate.swapchain = create-swapchain (window.get-drawable-size)
    if (msaa-enabled?)
        istate.swapchain-resolve-source =
            create-swapchain-resolve-source (window.get-drawable-size)

fn init ()
    raising noreturn

    wgpu.SetLogCallback
        fn (log-level message userdata)
            print ('from-rawstring String message)
        null
    wgpu.SetLogLevel cfg.wgpu-log-level

    local instance-extras : wgpu.InstanceExtras
        chain =
            wgpu.ChainedStruct
                sType = (bitcast wgpu.NativeSType.InstanceExtras wgpu.SType)
        backends = cfg.wgpu-low-level-api

    istate.instance =
        wgpu.CreateInstance
            &local wgpu.InstanceDescriptor
                nextInChain = &instance-extras as (mutable@ wgpu.ChainedStruct)

    istate.surface = (create-surface)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter istate.instance
        &local wgpu.RequestAdapterOptions
            compatibleSurface = ('rawptr istate.surface)
            powerPreference = copy cfg.power-preference
        fn (status result msg userdata)
            istate.adapter = result
            ;
        null

    local adapter-limits : wgpu.SupportedLimits
    wgpu.AdapterGetLimits istate.adapter &adapter-limits

    feature-count := wgpu.AdapterEnumerateFeatures istate.adapter null

    # TODO: add functions for querying supported features and enable the ones we want conditionally.
    supported-features := alloca-array wgpu.FeatureName feature-count
    wgpu.AdapterEnumerateFeatures istate.adapter supported-features

    local required-features =
        arrayof wgpu.FeatureName
            'Depth32FloatStencil8

    wgpu.AdapterRequestDevice istate.adapter
        &local wgpu.DeviceDescriptor
            requiredFeaturesCount = (countof required-features)
            requiredFeatures = &required-features
            requiredLimits =
                &local wgpu.RequiredLimits
                    limits =
                        wgpu.Limits
                            maxTextureDimension1D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureDimension2D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureDimension3D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxTextureArrayLayers = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxBindGroups = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxBindingsPerBindGroup = wgpu.WGPU_LIMIT_U32_UNDEFINED
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
                            maxInterStageShaderVariables = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxColorAttachments = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxColorAttachmentBytesPerSample = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupStorageSize = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeInvocationsPerWorkgroup = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupSizeX = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupSizeY = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupSizeZ = wgpu.WGPU_LIMIT_U32_UNDEFINED
                            maxComputeWorkgroupsPerDimension = wgpu.WGPU_LIMIT_U32_UNDEFINED
        fn (status result msg userdata)
            if (status != wgpu.RequestDeviceStatus.Success)
                print ('from-rawstring String msg)
            istate.device = result
            ;
        null

    local device-limits : wgpu.SupportedLimits
    wgpu.DeviceGetLimits istate.device &device-limits
    istate.limits = device-limits.limits

    wgpu.DeviceSetUncapturedErrorCallback istate.device
        fn (error-type msg userdata)
            raising noreturn
            print ('from-rawstring String msg)

            ET := wgpu.ErrorType

            switch error-type
            case ET.Validation
                assert false
            default ()
            ;
        null

    istate.swapchain = (create-swapchain (window.get-drawable-size))
    istate.swapchain-resolve-source = (create-swapchain-resolve-source (window.get-drawable-size))
    istate.queue = (wgpu.DeviceGetQueue istate.device)
    ;

fn set-clear-color (color)
    istate.clear-color = color

fn get-cmd-encoder ()
    using types

    cmd-encoder := 'force-unwrap istate.cmd-encoder
    imply cmd-encoder CommandEncoder

fn get-device ()
    deref istate.device

fn get-swapchain-image ()
    using types
    imply (view (deref ('force-unwrap istate.swapchain-image))) TextureView

fn get-swapchain-resolve-source ()
    using types
    try
        imply
            view (deref ('unwrap istate.swapchain-resolve-source))
            TextureView
    else (view (nullof TextureView))

fn get-msaa-sample-count ()
    deref cfg.msaa-samples

fn begin-frame ()
    using types

    if (window.minimized?)
        raise GPUError.OutdatedSwapchain

    swapchain-image := (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        raise GPUError.OutdatedSwapchain

    cmd-encoder := (wgpu.DeviceCreateCommandEncoder istate.device (&local wgpu.CommandEncoderDescriptor))

    # clear
    if (not (msaa-enabled?))
        'finish
            RenderPass cmd-encoder (ColorAttachment (view swapchain-image) none true istate.clear-color)
    else
        'finish
            RenderPass cmd-encoder (ColorAttachment (get-swapchain-resolve-source) (view swapchain-image) true istate.clear-color)

    istate.swapchain-image = swapchain-image
    istate.cmd-encoder = cmd-encoder

fn present ()
    using types

    cmd-encoder := imply ('force-unwrap ('swap istate.cmd-encoder none)) CommandEncoder
    'submit ('finish cmd-encoder)
    wgpu.SwapChainPresent istate.swapchain
    'swap istate.swapchain-image none
    ;

do
    let init update-render-area set-clear-color begin-frame present \
        get-info get-preferred-surface-format get-cmd-encoder get-device \
        get-swapchain-image get-swapchain-resolve-source \
        get-msaa-sample-count msaa-enabled? \

    let types

    locals;
