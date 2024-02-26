import C.stdlib
using import Array glm print radl.strfmt String struct
import ..logger sdl .types .wgpu ..window

from wgpu let typeinit@ chained@

# imports necessary to augment all types with their implementation
import .BindGroup .CommandEncoder .GPUBuffer .RenderPass .RenderPipeline .Sampler .ShaderModule .Texture

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

fn configure-surface ()
    width height := (window.get-size)
    ctx.surface-size = ivec2 width height
    wgpu.SurfaceConfigure ctx.surface
        typeinit@
            device = ctx.device
            usage = wgpu.TextureUsage.RenderAttachment
            format = (get-preferred-surface-format)
            width = (width as u32)
            height = (height as u32)
            presentMode = ctx.present-mode

fn create-msaa-resolve-source (width height)
    using types
    try
        TextureView
            Texture (u32 width) (u32 height) (get-preferred-surface-format) none
                render-target? = true
                sample-count = ctx.msaa? 4:u32 1:u32
    else
        logger.write-fatal "could not create MSAA resolve source"
        abort;

fn update-render-area ()
    configure-surface;
    if ctx.msaa?
        ctx.swapchain-resolve-source =
            create-msaa-resolve-source (window.get-drawable-size)

fn set-clear-color (color)
    ctx.clear-color = color

fn get-cmd-encoder ()
    using types

    cmd-encoder := 'force-unwrap ctx.cmd-encoder
    imply cmd-encoder CommandEncoder

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

fn... set-present-mode (present-mode : wgpu.PresentMode)
    ctx.present-mode = present-mode
    ctx.outdated-surface? = true

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

    ctx.instance =
        wgpu.CreateInstance
            chained@ 'InstanceExtras
                backends = wgpu.InstanceBackend.All
                flags = wgpu.InstanceFlag.Debug

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

    local required-features =
        arrayof wgpu.FeatureName
            'Depth32FloatStencil8

    wgpu.AdapterRequestDevice ctx.adapter
        typeinit@
            requiredFeatureCount = (countof required-features)
            requiredFeatures = &required-features
        fn (status result msg userdata)
            if (status != wgpu.RequestDeviceStatus.Success)
                print ('from-rawstring String msg)
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

    ctx.present-mode = cfg.present-mode
    ctx.msaa? = cfg.msaa?
    configure-surface;

    if ctx.msaa?
        ctx.msaa-resolve-source = (create-msaa-resolve-source (window.get-drawable-size))

    ctx.queue = (wgpu.DeviceGetQueue ctx.device)
    ;

fn begin-frame ()
    using types

    if ctx.outdated-surface?
        configure-surface;
        ctx.outdated-surface? = false
        raise GPUError.DiscardedFrame

    cmd-encoder := (wgpu.DeviceCreateCommandEncoder ctx.device (typeinit@))

    surface-texture := (acquire-surface-texture)
    surface-texture-view := (TextureView surface-texture)

    # clear
    if (not ctx.msaa?)
        'finish
            RenderPass cmd-encoder (ColorAttachment (view surface-texture-view) none true ctx.clear-color)
    else
        'finish
            RenderPass cmd-encoder (ColorAttachment (get-msaa-resolve-source) (view surface-texture-view) true ctx.clear-color)

    ctx.surface-texture = surface-texture
    ctx.surface-texture-view = surface-texture-view
    ctx.cmd-encoder = cmd-encoder

fn present ()
    using types

    cmd-encoder := imply ('force-unwrap ('swap ctx.cmd-encoder none)) CommandEncoder
    'submit ('finish cmd-encoder)
    wgpu.SurfacePresent ctx.surface
    ctx.surface-texture-view = none
    ctx.surface-texture = none
    ()

do
    let init update-render-area set-clear-color begin-frame present \
        get-preferred-surface-format get-cmd-encoder get-device \
        get-surface-texture get-msaa-resolve-source \
        msaa-enabled? get-present-mode set-present-mode

    let types

    locals;
