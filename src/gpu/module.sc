using import Array glm hash print radl.ext radl.strfmt String struct
import .constants ..logger .types .wgpu ..window

from wgpu let chained@ chained-out@

using import .common
using import ..context ..exceptions ..helpers

cfg := context-accessor 'config 'gpu
ctx := context-accessor 'gpu

inline wgpu-array-query (f args...)
    T@ := elementof (typeof f) (va-countof args...)
    T := elementof T@ 0

    local result : (Array T)
    count := f ((va-join args...) null)
    'resize result count

    ptr := 'data result
    f ((va-join args...) ptr)
    result

fn get-preferred-surface-format ()
    # wgpu.SurfaceGetPreferredFormat ctx.surface ctx.adapter
    # FIXME: the preferred format is now the first result from available formats
    wgpu.TextureFormat.BGRA8UnormSrgb

fn create-surface ()
    dispatch (window.get-native-info)
    case X11 (display window)
        wgpu.InstanceCreateSurface ctx.instance
            chained@ 'SurfaceSourceXlibWindow
                display = display
                window = typeinit window
    case Wayland (display surface)
        wgpu.InstanceCreateSurface ctx.instance
            chained@ 'SurfaceSourceWaylandSurface
                display = display
                surface = surface
    case Windows (hinstance hwnd)
        wgpu.InstanceCreateSurface ctx.instance
            chained@ 'SurfaceSourceWindowsHWND
                hinstance = hinstance
                hwnd = hwnd
    default
        abort;

fn create-msaa-resolve-source (width height)
    using types
    try
        TextureView
            Texture (u32 width) (u32 height) 1:u32 (get-preferred-surface-format) none
                render-target? = true
                sample-count = ctx.msaa? 4:u32 1:u32
    else
        logger.write-fatal "could not create MSAA resolve source"
        abort;

fn configure-surface ()
    width height := (window.get-drawable-size)
    ctx.surface-size = ivec2 width height
    ctx.scaled-surface-size = ivec2 (window.get-size)
    wgpu.SurfaceConfigure ctx.surface
        typeinit@
            device = ctx.device
            usage = wgpu.TextureUsage.RenderAttachment
            format = (get-preferred-surface-format)
            width = (width as u32)
            height = (height as u32)
            presentMode = ctx.present-mode
    if ctx.msaa?
        ctx.msaa-resolve-source =
            create-msaa-resolve-source width height
    ctx.outdated-surface? = false

fn set-clear-color (color)
    ctx.clear-color = color

fn get-cmd-encoder ()
    deref ctx.cmd-encoder

fn get-device ()
    deref ctx.device

fn get-surface-texture ()
    using types
    imply (view (deref ('force-unwrap ctx.surface-texture-view))) TextureView

fn get-msaa-resolve-source ()
    using types
    try
        imply
            view (deref ('unwrap ctx.msaa-resolve-source))
            TextureView
    else (view (nullof TextureView))

fn get-present-mode ()
    deref ctx.present-mode

fn... set-present-mode (present-mode : wgpu.PresentMode, reconfigure-surface? : bool = true)
    from wgpu let PresentMode
    modes := ctx.available-present-modes

    inline... check-mode (mode : PresentMode, fallback : PresentMode)
        mode fallback := imply mode wgpu.PresentMode, imply fallback wgpu.PresentMode
        if ('in? modes mode)
            mode
        elseif ('in? modes fallback)
            fallback
        else PresentMode.Fifo

    let selected-mode =
        switch present-mode
        case 'Immediate
            check-mode 'Immediate 'Mailbox
        case 'Mailbox
            check-mode 'Mailbox 'Immediate
        case 'FifoRelaxed
            check-mode 'FifoRelaxed 'Fifo
        default
            PresentMode.Fifo # always available

    if (present-mode != selected-mode)
        logger.write-warning f"${present-mode} present mode was unavailable, falling back to ${selected-mode}"

    ctx.present-mode = selected-mode
    ctx.outdated-surface? = reconfigure-surface?

fn msaa-enabled? ()
    deref ctx.msaa?

fn acquire-surface-texture ()
    using types

    local surface-texture : wgpu.SurfaceTexture
    wgpu.SurfaceGetCurrentTexture ctx.surface &surface-texture
    Status := wgpu.SurfaceGetCurrentTextureStatus

    if (surface-texture.status > Status.SuccessSuboptimal)
        logger.write-debug f"The request for the surface texture was unsuccessful: ${surface-texture.status}"

    switch surface-texture.status
    pass 'SuccessSuboptimal
    pass 'SuccessOptimal
    do
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
        fn (level message userdata)
            message := String message.data message.length
            switch level
            case 'Error
                logger.write-fatal message
            case 'Warn
                logger.write-warning message
            case 'Info
                logger.write-info message
            case 'Debug
                logger.write-debug message
            case 'Trace
                print message
            default ()
        null
    wgpu.SetLogLevel cfg.wgpu-log-level

    let instance-flags =
        if cfg.enable-validation?
            enum-bitfield wgpu.InstanceFlag u32
                'Debug
                'Validation
        else 0:u32

    ctx.instance =
        wgpu.CreateInstance
            chained@ 'InstanceExtras
                backends = cfg.wgpu-low-level-backend
                flags = instance-flags

    ctx.surface = (create-surface)

    wgpu.InstanceRequestAdapter ctx.instance
        typeinit@
            # compatibleSurface = ('rawptr ctx.surface)
            featureLevel = 'Core
            powerPreference = cfg.power-preference
        typeinit
            mode = 'AllowProcessEvents
            callback =
                fn (status adapter message userdata1 userdata2)
                    raising noreturn
                    if (status == 'Success)
                        ctx.adapter = adapter
                    else
                        logger.write-fatal
                            "Request for the graphics adapter failed. Verify you have the necessary drivers installed.\n"
                            f"Could not create adapter. ${(String message.data message.length)}"
                        abort;

    # NOTE: Currently not implemented
    # wgpu.InstanceProcessEvents ctx.instance

    do
        local caps : wgpu.SurfaceCapabilities
        wgpu.SurfaceGetCapabilities ctx.surface ctx.adapter &caps
        for i in (range caps.presentModeCount)
            'insert ctx.available-present-modes (caps.presentModes @ i)

        wgpu.SurfaceCapabilitiesFreeMembers caps

    set-present-mode cfg.present-mode false

    native-feature := (name) -> (bitcast (imply name wgpu.NativeFeature) wgpu.FeatureName)
    local required-features =
        arrayof wgpu.FeatureName
            'Depth32FloatStencil8
            native-feature 'PushConstants

    inline make-limits-struct ()
        wgpu.Limits
            nextInChain =
                bitcast
                    &local wgpu.NativeLimits
                        chain =
                            typeinit
                                sType = bitcast wgpu.NativeSType.NativeLimits wgpu.SType
                    mutable@ wgpu.ChainedStructOut

    local adapter-limits := (make-limits-struct)
    wgpu.AdapterGetLimits ctx.adapter &adapter-limits
    limits-extras := bitcast adapter-limits.nextInChain (mutable@ wgpu.NativeLimits)

    wgpu.AdapterRequestDevice ctx.adapter
        typeinit@
            requiredFeatureCount = (countof required-features)
            requiredFeatures = &required-features
            requiredLimits =
                chained-out@ 'NativeLimits
                    # customized values
                    maxPushConstantSize = constants.MAX_PUSH_CONSTANT_SIZE
                    .maxUniformBufferBindingSize = constants.MAX_UNIFORM_BUFFER_SIZE

                    # defaults
                    .maxTextureDimension1D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxTextureDimension2D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxTextureDimension3D = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxTextureArrayLayers = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxBindGroups = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxBindingsPerBindGroup = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxDynamicUniformBuffersPerPipelineLayout = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxDynamicStorageBuffersPerPipelineLayout = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxSampledTexturesPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxSamplersPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxStorageBuffersPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxStorageTexturesPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxUniformBuffersPerShaderStage = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxStorageBufferBindingSize = wgpu.WGPU_LIMIT_U64_UNDEFINED
                    .minUniformBufferOffsetAlignment = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .minStorageBufferOffsetAlignment = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxVertexBuffers = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxBufferSize = wgpu.WGPU_LIMIT_U64_UNDEFINED
                    .maxVertexAttributes = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxVertexBufferArrayStride = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxInterStageShaderVariables = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxColorAttachments = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxColorAttachmentBytesPerSample = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxComputeWorkgroupStorageSize = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxComputeInvocationsPerWorkgroup = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxComputeWorkgroupSizeX = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxComputeWorkgroupSizeY = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxComputeWorkgroupSizeZ = wgpu.WGPU_LIMIT_U32_UNDEFINED
                    .maxComputeWorkgroupsPerDimension = wgpu.WGPU_LIMIT_U32_UNDEFINED
            uncapturedErrorCallbackInfo = typeinit
                callback =
                    fn (device err message u1 u2)
                        msgstr := () -> (String message.data message.length)

                        switch err
                        pass 'Validation
                        pass 'OutOfMemory
                        pass 'Internal
                        pass 'Unknown
                        # FIXME: where is it?
                        # pass 'DeviceLost
                        do
                            logger.write-fatal "\n" (msgstr)
                            abort;
                        default
                            ()
        typeinit
            mode = 'AllowProcessEvents
            callback =
                fn (status result msg userdata1 userdata2)
                    if (status != wgpu.RequestDeviceStatus.Success)
                        logger.write-fatal (String msg.data msg.length)
                        abort;
                    ctx.device = result
                    ;

    # NOTE: Currenlty not implemented
    # wgpu.InstanceProcessEvents ctx.instance

    local device-limits := (make-limits-struct)
    wgpu.DeviceGetLimits ctx.device &device-limits
    limits-extras := bitcast device-limits.nextInChain (mutable@ wgpu.NativeLimits)

    ctx.supported-limits = adapter-limits
    ctx.requested-limits = device-limits

    ctx.msaa? = cfg.msaa?
    configure-surface;

    if ctx.msaa?
        ctx.msaa-resolve-source = (create-msaa-resolve-source (window.get-drawable-size))

    ctx.queue = (wgpu.DeviceGetQueue ctx.device)
    ctx.cmd-encoder = wgpu.DeviceCreateCommandEncoder ctx.device (typeinit@)

    'collect-info ctx.renderer-backend-info
    ;

fn begin-frame ()
    using types

    if ctx.outdated-surface?
        configure-surface;

    surface-texture := (acquire-surface-texture)
    surface-texture-view := (TextureView surface-texture)

    # clear
    if (not ctx.msaa?)
        'finish
            RenderPass ctx.cmd-encoder (ColorAttachment (view surface-texture-view) none true ctx.clear-color)
                label-suffix = "internal clear"
    else
        'finish
            RenderPass ctx.cmd-encoder (ColorAttachment (get-msaa-resolve-source) (view surface-texture-view) true ctx.clear-color)

    ctx.surface-texture = surface-texture
    ctx.surface-texture-view = surface-texture-view

fn present ()
    using types

    # immediately replace with the next encoder
    local cmd-encoder : CommandEncoder = wgpu.DeviceCreateCommandEncoder ctx.device (typeinit@)
    swap ctx.cmd-encoder cmd-encoder

    'submit ('finish cmd-encoder)
    wgpu.SurfacePresent ctx.surface
    ctx.surface-texture-view = none
    ctx.surface-texture = none

    ctx.in-flight-resources = (typeinit)
    ()

fn flag-surface-outdated ()
    ctx.outdated-surface? = true

fn generate-report ()
    local global-report : wgpu.GlobalReport
    wgpu.GenerateReport ctx.instance &global-report

    global-report.hub

inline make-resource-cache-get (cache)
    inline get-internal-resource (k makef args...)
        'get (getattr ctx.internal-resources cache) k makef args...

do
    let init set-clear-color begin-frame present \
        get-preferred-surface-format get-cmd-encoder get-device \
        get-surface-texture get-msaa-resolve-source \
        msaa-enabled? get-present-mode set-present-mode
    let flag-surface-outdated
    let generate-report

    get-internal-texture := make-resource-cache-get 'textures
    get-internal-sampler := make-resource-cache-get 'samplers
    get-internal-bind-group := make-resource-cache-get 'bind-groups
    get-internal-bind-group-layout := make-resource-cache-get 'bind-group-layouts
    get-internal-pipeline-layout := make-resource-cache-get 'pipeline-layouts
    get-internal-pipeline := make-resource-cache-get 'pipelines

    let types

    locals;
