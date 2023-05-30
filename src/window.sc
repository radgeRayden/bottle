from (import .config) let istate-cfg
cfg := `istate-cfg.window
run-stage;

using import String
import sdl

global handle : (mutable@ sdl.Window)

fn get-handle ()
    handle

fn get-native-info ()
    local info : sdl.SysWMinfo
    sdl.SDL_VERSION &info.version

    assert (storagecast (sdl.GetWindowWMInfo handle &info))

    let info = info.info

    # FIXME: use the window subsystem enum properly
    static-match operating-system
    case 'linux
        _ info.x11.display info.x11.window
    case 'windows
        _ info.win.hinstance info.win.window
    default
        error "OS not supported"

fn get-size ()
    local w : i32
    local h : i32
    sdl.GetWindowSize handle &w &h
    _ w h

fn get-drawable-size ()
    local w : i32
    local h : i32
    sdl.GetWindowSizeInPixels handle &w &h
    _ w h

fn get-desktop-size (display)
    local mode : sdl.DisplayMode
    sdl.GetDesktopDisplayMode display &mode
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
        (sdl.GetWindowFlags handle) & sdl.SDL_WINDOW_MINIMIZED
        bool

fn init ()
    if (operating-system == 'windows)
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_AWARENESS "permonitorv2"
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_SCALING "0"

    # FIXME: maybe SDL initialization shouldn't be in the window module as we might want to make it optional in the future.
    sdl.Init
        sdl.SDL_INIT_EVERYTHING

    relative-width relative-height := (get-relative-size 0 cfg.relative-width cfg.relative-height)
    let width =
        try (deref ('unwrap cfg.width))
        else relative-width
    let height =
        try (deref ('unwrap cfg.height))
        else relative-height

    handle =
        sdl.CreateWindow
            cfg.title
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            width
            height
            sdl.SDL_WINDOW_RESIZABLE

    assert (handle != null) (.. "Error while creating window:" (String (sdl.GetError)))
    ;

fn shutdown ()
    sdl.DestroyWindow handle
    sdl.Quit;

do
    let
        get-handle
        get-native-info

        init
        get-size
        get-drawable-size
        get-desktop-scaling-factor
        minimized?
        shutdown
    locals;
