using import enum

sdl := import sdl3

enum MouseButton plain
    Left    = sdl.SDL_BUTTON_LEFT
    Middle  = sdl.SDL_BUTTON_MIDDLE
    Right   = sdl.SDL_BUTTON_RIGHT
    X1      = sdl.SDL_BUTTON_X1
    X2      = sdl.SDL_BUTTON_X2

do
    let MouseButton
    locals;
