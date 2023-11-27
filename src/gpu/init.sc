import C.stdlib
using import print
using import String
using import struct
using import radl.strfmt

import sdl
import .wgpu
import .types
import ..logger

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
    dispatch (window.get-native-info)
    case X11 (display window)
        wgpu.InstanceCreateSurface istate.instance
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromXlibWindow
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromXlibWindow
                            display = display
                            window = typeinit window
                        mutable@ wgpu.ChainedStruct
    case Wayland (display surface)
        wgpu.InstanceCreateSurface istate.instance
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromWaylandSurface
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromWaylandSurface
                            display = display
                            surface = surface
                        mutable@ wgpu.ChainedStruct
    case Windows (hinstance hwnd)
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
        abort;

fn configure-surface ()
    width height := (window.get-size)
    wgpu.SurfaceConfigure istate.surface
        &local wgpu.SurfaceConfiguration
            device = istate.device
            usage = wgpu.TextureUsage.RenderAttachment
            format = (get-preferred-surface-format)
            width = (width as u32)
            height = (height as u32)
            presentMode = istate.present-mode

fn create-msaa-resolve-source (width height)
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
    configure-surface;
    if (msaa-enabled?)
        istate.swapchain-resolve-source =
            create-msaa-resolve-source (window.get-drawable-size)

fn set-clear-color (color)
    istate.clear-color = color

fn get-cmd-encoder ()
    using types

    cmd-encoder := 'force-unwrap istate.cmd-encoder
    imply cmd-encoder CommandEncoder

fn get-device ()
    deref istate.device

fn get-surface-texture ()
    using types
    imply (view (deref ('force-unwrap istate.surface-texture-view))) TextureView

fn get-msaa-resolve-source ()
    using types
    try
        imply
            view (deref ('unwrap istate.msaa-resolve-source))
            TextureView
    else (view (nullof TextureView))

fn get-msaa-sample-count ()
    deref cfg.msaa-samples

fn get-present-mode ()
    deref istate.present-mode

fn... set-present-mode (present-mode : wgpu.PresentMode)
    istate.present-mode = present-mode
    istate.reconfigure-surface? = true

fn acquire-surface-texture ()
    using types

    local surface-texture : wgpu.SurfaceTexture
    wgpu.SurfaceGetCurrentTexture istate.surface &surface-texture

    if (surface-texture.status != 'Success)
        logger.write-debug f"The request for the surface texture was unsuccessful: ${surface-texture.status}"

    switch surface-texture.status
    case 'Success
        imply surface-texture.texture Texture
    pass 'Timeout
    pass 'Outdated
    pass 'Lost
    do
        if (surface-texture.texture != null)
            wgpu.TextureRelease surface-texture.texture
        configure-surface;

        raise GPUError.DiscardedFrame
    default
        logger.write-fatal "Could not acquire surface texture: ${surface-texture.status}"
        abort;

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

    wgpu.InstanceRequestAdapter istate.instance
        &local wgpu.RequestAdapterOptions
            compatibleSurface = ('rawptr istate.surface)
            powerPreference = copy cfg.power-preference
        fn (status result msg userdata)
            # FIXME: specify backend in error message
            if (status != wgpu.RequestAdapterStatus.Success)
                logger.write-fatal "Request for the graphics adapter failed. Verify you have the necessary drivers installed."
                print2 "WebGPU says:"
                print2 ('from-rawstring String msg)
                abort;

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
            requiredFeatureCount = (countof required-features)
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

    istate.present-mode = cfg.present-mode
    configure-surface;
    if (msaa-enabled?)
        istate.msaa-resolve-source = (create-msaa-resolve-source (window.get-drawable-size))

    istate.queue = (wgpu.DeviceGetQueue istate.device)
    ;

fn begin-frame ()
    using types

    if istate.reconfigure-surface?
        configure-surface;
        istate.reconfigure-surface? = false
        raise GPUError.DiscardedFrame

    cmd-encoder := (wgpu.DeviceCreateCommandEncoder istate.device (&local wgpu.CommandEncoderDescriptor))

    surface-texture := (acquire-surface-texture)
    surface-texture-view := (TextureView surface-texture)

    # clear
    if (not (msaa-enabled?))
        'finish
            RenderPass cmd-encoder (ColorAttachment (view surface-texture-view) none true istate.clear-color)
    else
        'finish
            RenderPass cmd-encoder (ColorAttachment (get-msaa-resolve-source) (view surface-texture-view) true istate.clear-color)

    istate.surface-texture = surface-texture
    istate.surface-texture-view = surface-texture-view
    istate.cmd-encoder = cmd-encoder

fn present ()
    using types

    cmd-encoder := imply ('force-unwrap ('swap istate.cmd-encoder none)) CommandEncoder
    'submit ('finish cmd-encoder)
    wgpu.SurfacePresent istate.surface
    istate.surface-texture-view = none
    istate.surface-texture = none
    ()

do
    let init update-render-area set-clear-color begin-frame present \
        get-info get-preferred-surface-format get-cmd-encoder get-device \
        get-surface-texture get-msaa-resolve-source \
        get-msaa-sample-count msaa-enabled? \
        get-present-mode set-present-mode

    let types

    locals;
