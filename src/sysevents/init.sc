using import struct

import sdl
import .callbacks
import ..gpu
import ..enums

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
    from enums let Key MouseButton

    local event : sdl.Event
    while (sdl.PollEvent &event)
        switch event.type
        case sdl.SDL_QUIT
            let result = (callbacks.quit)
            istate.really-quit? =
                (none? result) or (imply result bool)

        case sdl.SDL_KEYDOWN
            callbacks.key-pressed (event.key.keysym.sym as Key)

        case sdl.SDL_KEYUP
            callbacks.key-released (event.key.keysym.sym as Key)

        case sdl.SDL_MOUSEMOTION
            edata := event.motion
            callbacks.mouse-moved edata.x edata.y edata.xrel edata.yrel

        case sdl.SDL_MOUSEBUTTONDOWN
            edata := event.button
            callbacks.mouse-pressed (edata.button as i32 as MouseButton) edata.x edata.y edata.clicks

        case sdl.SDL_MOUSEBUTTONUP
            edata := event.button
            callbacks.mouse-released (edata.button as i32 as MouseButton) edata.x edata.y edata.clicks

        case sdl.SDL_MOUSEWHEEL
            edata := event.wheel
            callbacks.wheel-scrolled edata.preciseX edata.preciseY

        case sdl.SDL_TEXTINPUT
            edata := event.text
            callbacks.text-input edata.text

        case sdl.SDL_WINDOWEVENT
            switch event.window.event
            case sdl.SDL_WINDOWEVENT_RESIZED
                gpu.update-render-area;
            case sdl.SDL_WINDOWEVENT_RESTORED
                gpu.update-render-area;
            default
                ;
        default
            ;

do
    let
        really-quit!
        really-quit?
        quit
        dispatch
    locals;
