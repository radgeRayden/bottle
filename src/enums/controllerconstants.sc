using import enum
import sdl

enum ControllerAxis : u8
    LeftX = sdl.SDL_CONTROLLER_AXIS_LEFTX
    LeftY = sdl.SDL_CONTROLLER_AXIS_LEFTY
    RightX = sdl.SDL_CONTROLLER_AXIS_RIGHTX
    RightY = sdl.SDL_CONTROLLER_AXIS_RIGHTY
    TriggerLeft = sdl.SDL_CONTROLLER_AXIS_TRIGGERLEFT
    TriggerRight = sdl.SDL_CONTROLLER_AXIS_TRIGGERRIGHT

    inline __imply (lhsT rhsT)
        static-if (rhsT == sdl.GameControllerAxis)
            inline (self)
                bitcast ((storagecast self) as i32) sdl.GameControllerAxis
        else
            super-type.__imply lhsT rhsT

enum ControllerButton : u8
    A = sdl.SDL_CONTROLLER_BUTTON_A
    B = sdl.SDL_CONTROLLER_BUTTON_B
    X = sdl.SDL_CONTROLLER_BUTTON_X
    Y = sdl.SDL_CONTROLLER_BUTTON_Y
    Back = sdl.SDL_CONTROLLER_BUTTON_BACK
    Guide = sdl.SDL_CONTROLLER_BUTTON_GUIDE
    Start = sdl.SDL_CONTROLLER_BUTTON_START
    LeftStick = sdl.SDL_CONTROLLER_BUTTON_LEFTSTICK
    RightStick = sdl.SDL_CONTROLLER_BUTTON_RIGHTSTICK
    LeftBumper = sdl.SDL_CONTROLLER_BUTTON_LEFTSHOULDER
    RightBumper = sdl.SDL_CONTROLLER_BUTTON_RIGHTSHOULDER
    Up = sdl.SDL_CONTROLLER_BUTTON_DPAD_UP
    Down = sdl.SDL_CONTROLLER_BUTTON_DPAD_DOWN
    Left = sdl.SDL_CONTROLLER_BUTTON_DPAD_LEFT
    Right = sdl.SDL_CONTROLLER_BUTTON_DPAD_RIGHT
    Misc1 = sdl.SDL_CONTROLLER_BUTTON_MISC1
    Paddle1 = sdl.SDL_CONTROLLER_BUTTON_PADDLE1
    Paddle2 = sdl.SDL_CONTROLLER_BUTTON_PADDLE2
    Paddle3 = sdl.SDL_CONTROLLER_BUTTON_PADDLE3
    Paddle4 = sdl.SDL_CONTROLLER_BUTTON_PADDLE4
    Touchpad = sdl.SDL_CONTROLLER_BUTTON_TOUCHPAD

    inline __imply (lhsT rhsT)
        static-if (rhsT == sdl.GameControllerButton)
            inline (self)
                bitcast ((storagecast self) as i32) sdl.GameControllerButton
        else
            super-type.__imply lhsT rhsT

do
    let ControllerAxis ControllerButton
    locals;
