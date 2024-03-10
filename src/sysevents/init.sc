using import ..context struct
import .callbacks ..gpu ..enums ..imgui ..window

sdl := import sdl3
ctx := context-accessor 'sysevents

fn really-quit! ()
    ctx.application-quit? = true

fn really-quit? ()
    ctx.application-quit?

fn quit ()
    local ev : sdl.Event
    ev.type = sdl.EventType.QUIT
    sdl.PushEvent &ev
    ;

inline dispatch (handler)
    from enums let KeyboardKey MouseButton ControllerButton ControllerAxis

    local event : sdl.Event
    while (sdl.PollEvent &event)
        if (handler &event)
            continue;

        switch (event.type as sdl.EventType)
        case 'QUIT
            let result = (callbacks.quit)
            ctx.application-quit? =
                (none? result) or (imply result bool)

        case 'KEY_DOWN
            if (not event.key.repeat)
                callbacks.key-pressed (event.key.keysym.sym as KeyboardKey)

        case 'KEY_UP
            if (not event.key.repeat)
                callbacks.key-released (event.key.keysym.sym as KeyboardKey)

        case 'MOUSE_MOTION
            edata := event.motion
            callbacks.mouse-moved edata.x edata.y edata.xrel edata.yrel

        case 'MOUSE_BUTTON_DOWN
            edata := event.button
            callbacks.mouse-pressed (edata.button as i32 as MouseButton) edata.x edata.y edata.clicks

        case 'MOUSE_BUTTON_UP
            edata := event.button
            callbacks.mouse-released (edata.button as i32 as MouseButton) edata.x edata.y edata.clicks

        case 'MOUSE_WHEEL
            edata := event.wheel
            callbacks.wheel-scrolled edata.mouse_x edata.mouse_y

        case 'TEXT_INPUT
            edata := event.text
            callbacks.text-input edata.text

        case 'WINDOW_PIXEL_SIZE_CHANGED
            gpu.flag-surface-outdated;

        case 'WINDOW_ENTER_FULLSCREEN
            window._update-fullscreen-flag true

        case 'WINDOW_LEAVE_FULLSCREEN
            window._update-fullscreen-flag false

        case 'GAMEPAD_ADDED
            callbacks.controller-added event.cdevice.which

        case 'GAMEPAD_REMOVED
            callbacks.controller-removed event.cdevice.which

        case 'GAMEPAD_BUTTON_DOWN
            edata := event.gbutton
            callbacks.controller-button-pressed edata.which (edata.button as ControllerButton)

        case 'GAMEPAD_BUTTON_UP
            edata := event.gbutton
            callbacks.controller-button-released edata.which (edata.button as ControllerButton)

        case 'GAMEPAD_AXIS_MOTION
            edata := event.gaxis
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
