using import struct

let sdl = (import ..FFI.sdl)

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
    local event : sdl.Event
    while (sdl.PollEvent &event)
        if (event.type == sdl.SDL_QUIT)
            really-quit!;

do
    let
        really-quit!
        really-quit?
        quit
        dispatch
    locals;
