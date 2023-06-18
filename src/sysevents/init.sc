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

inline dispatch (handler)
    from enums let KeyboardKey MouseButton ControllerButton ControllerAxis

    local event : sdl.Event
    while (sdl.PollEvent &event)
        if (handler &event)
            continue;

        switch event.type
        case sdl.SDL_QUIT
            let result = (callbacks.quit)
            istate.really-quit? =
                (none? result) or (imply result bool)

        case sdl.SDL_KEYDOWN
            if (not event.key.repeat)
                callbacks.key-pressed (event.key.keysym.sym as KeyboardKey)

        case sdl.SDL_KEYUP
            if (not event.key.repeat)
                callbacks.key-released (event.key.keysym.sym as KeyboardKey)

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

        case sdl.SDL_CONTROLLERDEVICEADDED
            callbacks.controller-added event.cdevice.which

        case sdl.SDL_CONTROLLERDEVICEREMOVED
            callbacks.controller-removed event.cdevice.which

        case sdl.SDL_CONTROLLERBUTTONDOWN
            edata := event.cbutton
            callbacks.controller-button-pressed edata.which (edata.button as ControllerButton)

        case sdl.SDL_CONTROLLERBUTTONUP
            edata := event.cbutton
            callbacks.controller-button-released edata.which (edata.button as ControllerButton)

        case sdl.SDL_CONTROLLERAXISMOTION
            edata := event.caxis
            callbacks.controller-axis-moved edata.which (edata.axis as ControllerAxis) edata.value

        default
            ;

do
    let
        really-quit!
        really-quit?
        quit
        dispatch
    locals;
