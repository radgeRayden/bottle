let sdl = (import .FFI.sdl)

fn init ()
    sdl.Init
        sdl.SDL_INIT_VIDEO

    local window =
        sdl.CreateWindow
            "Game from Scratch Re:Birth"
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            640
            480
            sdl.SDL_WINDOW_RESIZABLE
    ;

do
    let init
    locals;
