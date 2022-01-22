let sdl = (import .FFI.sdl)

global handle : (mutable@ sdl.Window)

fn get-handle ()
    handle

fn get-native-info ()
    local info : sdl.SysWMinfo
    sdl.SDL_VERSION &info.version

    assert (sdl.GetWindowWMInfo handle &info)

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

fn init ()
    sdl.Init
        sdl.SDL_INIT_VIDEO

    handle =
        sdl.CreateWindow
            "Game from Scratch Re:Birth"
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            640
            480
            sdl.SDL_WINDOW_RESIZABLE
    ;

do
    let
        get-handle
        get-native-info

        init
        get-size
    locals;
