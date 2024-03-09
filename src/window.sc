using import enum logger print radl.strfmt String struct
import .logger sdl3

using import .context
sdl := sdl3
cfg := context-accessor 'config 'window
platform-cfg := context-accessor 'config 'platform

struct BottleWindowState plain
    handle : (mutable@ sdl.Window)
    fullscreen? : bool

global istate : BottleWindowState

inline get-handle ()
    istate.handle

enum WindowNativeInfo
    Windows : (hinstance = voidstar) (hwnd = voidstar)
    X11 : (display = voidstar) (window = u64)
    Wayland : (display = voidstar) (surface = voidstar)

fn get-native-info ()
    props := sdl.GetWindowProperties (get-handle)
    if (props == 0)
        logger.write-fatal f"Could not query window properties: ${(sdl.GetError)}"
        abort;

    inline prop (p)
        prop := sdl.GetProperty props p null
        if (prop == null)
            logger.write-fatal f"${p} ${(sdl.GetError)}"
            abort;
        prop

    static-match operating-system
    case 'linux
        video-driver := 'from-rawstring String (sdl.GetCurrentVideoDriver)
        match video-driver
        case "x11"
            WindowNativeInfo.X11
                prop sdl.SDL_PROP_WINDOW_X11_DISPLAY_POINTER
                (sdl.GetNumberProperty props sdl.SDL_PROP_WINDOW_X11_WINDOW_NUMBER 0) as u64
        case "wayland"
            WindowNativeInfo.Wayland
                prop sdl.SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER
                prop sdl.SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER
        default
            logger.write-fatal f"Unsupported SDL_video driver: ${video-driver}"
            abort;
    case 'windows
        WindowNativeInfo.Windows
            prop sdl.SDL_PROP_WINDOW_WIN32_INSTANCE_POINTER
            prop sdl.SDL_PROP_WINDOW_WIN32_WINDOW_POINTER
    default
        static-error "OS not supported"

fn get-size ()
    local w : i32
    local h : i32
    sdl.GetWindowSize (get-handle) &w &h
    _ w h

fn get-drawable-size ()
    local w : i32
    local h : i32
    sdl.GetWindowSizeInPixels (get-handle) &w &h
    _ w h

fn get-desktop-size ()
    display := (sdl.GetPrimaryDisplay)
    if (display == 0:u32)
        logger.write-fatal (sdl.GetError)
        abort;

    mode := sdl.GetCurrentDisplayMode display
    assert (mode != null)
    _ mode.w mode.h

fn get-display-size (display)
    # FIXME
#     local mode : sdl.DisplayMode
#     sdl.GetCurrentDisplayMode display &mode
#     _ mode.w mode.h

fn get-desktop-scaling-factor ()
    let scaled = (get-size)
    let drawable = (get-drawable-size)
    drawable / scaled

fn get-relative-size (wratio hratio)
    dw dh := (get-desktop-size)
    va-map i32 ((f32 dw) * wratio) ((f32 dh) * hratio)

fn minimized? ()
    as
        (sdl.GetWindowFlags (get-handle)) & sdl.SDL_WINDOW_MINIMIZED
        bool

fn set-title (title)
    sdl.SetWindowTitle (get-handle) (title as rawstring)
    ()

fn get-title ()
    String (sdl.GetWindowTitle (get-handle))

fn set-fullscreen (value)
    if ((sdl.SetWindowFullscreen (get-handle) value) != 0)
        logger.write-debug f"Failed to set window to fullscreen: ${(sdl.GetError)}"

fn fullscreen? ()
    deref istate.fullscreen?

fn toggle-fullscreen ()
    set-fullscreen (not (fullscreen?))

# TODO: remove this once we migrate state to context.sc
fn _update-fullscreen-flag (value)
    istate.fullscreen? = value

fn set-icon (image-data)
    # sdl.SetWindowIcon (get-handle)
    #     sdl.CreateRGBSurface 0 (i32 image-data.width) (i32 image-data.height) 32 \
    #         0xFF000000 0x00FF0000 0x0000FF00 0x000000FF
    ()

fn init ()
    static-match operating-system
    case 'windows
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_AWARENESS "permonitorv2"
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_SCALING "0"
    case 'linux
        sdl.SetHint sdl.SDL_HINT_APP_ID platform-cfg.app-id
        if platform-cfg.force-x11?
            sdl.SetHint sdl.SDL_HINT_VIDEO_DRIVER "x11"
        else
            sdl.SetHint sdl.SDL_HINT_VIDEO_DRIVER "wayland,x11"
    default ()

    status :=
        sdl.Init
            | sdl.SDL_INIT_VIDEO sdl.SDL_INIT_TIMER sdl.SDL_INIT_GAMEPAD

    if (status < 0)
        msg := (sdl.GetError)
        logger.write-fatal f"SDL initialization failed: ${msg}"
        abort;

    relative-width relative-height := (get-relative-size cfg.relative-width cfg.relative-height)
    let width =
        try (deref ('unwrap cfg.width))
        else relative-width
    let height =
        try (deref ('unwrap cfg.height))
        else relative-height

    inline window-flags (flags...)
        va-lfold 0:u32
            inline (k next result)
                setting := getattr cfg k
                if setting
                    result | next
                else
                    result
            flags...

    handle :=
        sdl.CreateWindow
            cfg.title
            width
            height
            | sdl.SDL_WINDOW_HIGH_PIXEL_DENSITY
                window-flags
                    fullscreen? = sdl.SDL_WINDOW_FULLSCREEN
                    hidden? = sdl.SDL_WINDOW_HIDDEN
                    borderless? = sdl.SDL_WINDOW_BORDERLESS
                    resizable? = sdl.SDL_WINDOW_RESIZABLE
                    minimized? = sdl.SDL_WINDOW_MINIMIZED
                    maximized? = sdl.SDL_WINDOW_MAXIMIZED
                    always-on-top? = sdl.SDL_WINDOW_ALWAYS_ON_TOP

    if (handle == null)
        # TODO: unify error handling
        msg := (sdl.GetError)
        logger.write-fatal f"Error while creating window: ${msg}"
        abort;

    sdl.SyncWindow handle
    actual-flags := sdl.GetWindowFlags handle
    istate =
        typeinit
            handle = handle
            fullscreen? = bool (actual-flags & sdl.SDL_WINDOW_FULLSCREEN)
    ;

fn shutdown ()
    sdl.DestroyWindow (get-handle)
    sdl.Quit;

do
    let
        get-handle
        get-native-info

        init
        fullscreen?
        set-fullscreen
        toggle-fullscreen
        get-size
        get-drawable-size
        get-desktop-scaling-factor
        set-title
        get-title
        set-icon
        minimized?
        shutdown
        _update-fullscreen-flag

    let WindowNativeInfo

    locals;
