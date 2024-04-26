using import Array glm hash print radl.ext radl.strfmt String struct
import ..logger .types .wgpu ..window

from wgpu let chained@

using import .common
using import ..context ..exceptions

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
    wgpu.SurfaceGetPreferredFormat ctx.surface ctx.adapter

fn create-surface ()
    dispatch (window.get-native-info)
    case X11 (display window)
        wgpu.InstanceCreateSurface ctx.instance
            chained@ 'SurfaceDescriptorFromXlibWindow
                display = display
                window = typeinit window
    case Wayland (display surface)
        wgpu.InstanceCreateSurface ctx.instance
            chained@ 'SurfaceDescriptorFromWaylandSurface
                display = display
                surface = surface
    case Windows (hinstance hwnd)
        wgpu.InstanceCreateSurface ctx.instance
            chained@ 'SurfaceDescriptorFromWindowsHWND
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
        fn (level message userdata)
            message := 'from-rawstring String message
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
            compatibleSurface = ('rawptr ctx.surface)
            powerPreference = cfg.power-preference
        fn (status adapter message userdata)
            if (status == 'Success)
                ctx.adapter = adapter
            else
                logger.write-fatal
                    "Request for the graphics adapter failed. Verify you have the necessary drivers installed.\n"
                    f"Could not create adapter. ${message}"
                abort;
        null

    do
        local caps : wgpu.SurfaceCapabilities
        wgpu.SurfaceGetCapabilities ctx.surface ctx.adapter &caps
        for i in (range caps.presentModeCount)
            'insert ctx.available-present-modes (caps.presentModes @ i)

        wgpu.SurfaceCapabilitiesFreeMembers caps

    set-present-mode cfg.present-mode false

    local required-features =
        arrayof wgpu.FeatureName
            'Depth32FloatStencil8

    wgpu.AdapterRequestDevice ctx.adapter
        typeinit@
            requiredFeatureCount = (countof required-features)
            requiredFeatures = &required-features
        fn (status result msg userdata)
            if (status != wgpu.RequestDeviceStatus.Success)
                logger.write-fatal ('from-rawstring String msg)
                abort;
            ctx.device = result
            ;
        null

    local device-limits : wgpu.SupportedLimits
    wgpu.DeviceGetLimits ctx.device &device-limits
    ctx.limits = device-limits.limits

    wgpu.DeviceSetUncapturedErrorCallback ctx.device
        fn (err message userdata)
            msgstr := () -> ('from-rawstring String message)

            switch err
            pass 'Validation
            pass 'OutOfMemory
            pass 'Internal
            pass 'Unknown
            pass 'DeviceLost
            do
                logger.write-fatal "\n" (msgstr)
                abort;
            default
                ()
        null

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
    ()

fn flag-surface-outdated ()
    ctx.outdated-surface? = true

fn generate-report ()
    local global-report : wgpu.GlobalReport
    wgpu.GenerateReport ctx.instance &global-report

    switch ctx.renderer-backend-info.low-level-backend
    case 'D3D12
        global-report.dx12
    case 'Metal
        global-report.metal
    case 'Vulkan
        global-report.vulkan
    case 'OpenGL
        global-report.gl
    default
        logger.write-fatal "unsupported renderer backend"
        abort;

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
