using import struct

let sdl = (import ..FFI.sdl)
import .callbacks

struct SysEventsState
    really-quit? : bool

global istate : SysEventsState

fn really-quit! ()
    istate.really-quit? = true

fn really-quit? ()
    istate.really-quit?

fn quit ()
    local ev : sdl.Event
    ev.type = sdl.SDL_QUIT
    sdl.PushEvent &ev
    ;

fn dispatch ()
    using import .keyconstants

    local event : sdl.Event
    while (sdl.PollEvent &event)
        switch event.type
        case sdl.SDL_QUIT
            let result = (callbacks.on-quit)
            istate.really-quit? =
                (none? result) or (imply result bool)
        case sdl.SDL_KEYDOWN
            callbacks.on-key-pressed (event.key.keysym.sym as Key)
        case sdl.SDL_KEYUP
            callbacks.on-key-released (event.key.keysym.sym as Key)
        default
            ;

do
    let
        really-quit!
        really-quit?
        quit
        dispatch
    locals;
