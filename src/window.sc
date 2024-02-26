using import enum print radl.strfmt String struct
import .logger sdl

using import .context
cfg := context-accessor 'config 'window

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
    local wminfo : sdl.SysWMinfo
    sdl.SDL_VERSION &wminfo.version

    assert (storagecast (sdl.GetWindowWMInfo (get-handle) &wminfo))
    info subsystem := wminfo.info, wminfo.subsystem

    static-match operating-system
    case 'linux
        switch subsystem
        case 'SDL_SYSWM_X11
            WindowNativeInfo.X11 info.x11.display info.x11.window
        case 'SDL_SYSWM_WAYLAND
            WindowNativeInfo.Wayland info.wl.display info.wl.surface
        default
            logger.write-fatal f"Unsupported windowing system: ${subsystem}"
            abort;
    case 'windows
        assert (subsystem == 'SDL_SYSWM_WINDOWS)
        WindowNativeInfo.Windows info.win.hinstance info.win.window
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

fn get-desktop-size (display)
    local mode : sdl.DisplayMode
    result := sdl.GetDesktopDisplayMode display &mode

    if (result != 0)
        msg := (sdl.GetError)
        print "Failed to get desktop size:" ('from-rawstring String msg)
        return 1366 768

    _ mode.w mode.h

fn get-display-size (display)
    local mode : sdl.DisplayMode
    sdl.GetCurrentDisplayMode display &mode
    _ mode.w mode.h

fn get-desktop-scaling-factor ()
    let scaled = (get-size)
    let drawable = (get-drawable-size)
    drawable / scaled

fn get-relative-size (display wratio hratio)
    dw dh := get-desktop-size display
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
    istate.fullscreen? = value
    sdl.SetWindowFullscreen (get-handle)
        ? value sdl.SDL_WINDOW_FULLSCREEN_DESKTOP (bitcast 0:u32 sdl.WindowFlags)

fn fullscreen? ()
    deref istate.fullscreen?

fn toggle-fullscreen ()
    set-fullscreen (not (fullscreen?))

fn init ()
    if (operating-system == 'windows)
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_AWARENESS "permonitorv2"
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_SCALING "0"

    status :=
        sdl.Init
            | sdl.SDL_INIT_VIDEO sdl.SDL_INIT_TIMER sdl.SDL_INIT_GAMECONTROLLER

    if (status < 0)
        msg := (sdl.GetError)
        print "SDL initialization failed:" ('from-rawstring String msg)

    relative-width relative-height := (get-relative-size 0 cfg.relative-width cfg.relative-height)
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
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            width
            height
            window-flags
                fullscreen? = sdl.SDL_WINDOW_FULLSCREEN_DESKTOP
                hidden? = sdl.SDL_WINDOW_HIDDEN
                borderless? = sdl.SDL_WINDOW_BORDERLESS
                resizable? = sdl.SDL_WINDOW_RESIZABLE
                minimized? = sdl.SDL_WINDOW_MINIMIZED
                maximized? = sdl.SDL_WINDOW_MAXIMIZED
                always-on-top? = sdl.SDL_WINDOW_ALWAYS_ON_TOP

    if (handle == null)
        # TODO: unify error handling
        msg := (sdl.GetError)
        assert false (.. "Error while creating window:" ('from-rawstring String msg))

    istate =
        typeinit
            handle = handle
            fullscreen? = cfg.fullscreen?
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
        minimized?
        shutdown

    let WindowNativeInfo

    locals;
