import sdl

from (import .config) let istate-cfg

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

fn get-desktop-scaling-factor ()
    let scaled = (get-size)
    let drawable = (get-drawable-size)
    drawable / scaled

fn minimized? ()
    as
        (sdl.GetWindowFlags handle) & sdl.SDL_WINDOW_MINIMIZED
        bool

fn init ()
    cfg := istate-cfg.window

    if (operating-system == 'windows)
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_AWARENESS "permonitorv2"
        sdl.SetHint sdl.SDL_HINT_WINDOWS_DPI_SCALING "0"

    sdl.Init
        sdl.SDL_INIT_VIDEO

    handle =
        sdl.CreateWindow
            cfg.title
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            cfg.width
            cfg.height
            sdl.SDL_WINDOW_RESIZABLE
    ;

do
    let
        get-handle
        get-native-info

        init
        get-size
        get-drawable-size
        get-desktop-scaling-factor
        minimized?
    locals;
