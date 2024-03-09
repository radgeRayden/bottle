using import enum
sdl := import sdl3

enum ControllerAxis : u8
    LeftX = sdl.SDL_GAMEPAD_AXIS_LEFTX
    LeftY = sdl.SDL_GAMEPAD_AXIS_LEFTY
    RightX = sdl.SDL_GAMEPAD_AXIS_RIGHTX
    RightY = sdl.SDL_GAMEPAD_AXIS_RIGHTY
    LeftTrigger = sdl.SDL_GAMEPAD_AXIS_LEFT_TRIGGER
    RightTrigger = sdl.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER

    inline __imply (lhsT rhsT)
        static-if (rhsT == sdl.GamepadAxis)
            inline (self)
                bitcast ((storagecast self) as i32) sdl.GamepadAxis
        else
            super-type.__imply lhsT rhsT

enum ControllerButton : u8
    A = sdl.SDL_GAMEPAD_BUTTON_SOUTH
    B = sdl.SDL_GAMEPAD_BUTTON_EAST
    X = sdl.SDL_GAMEPAD_BUTTON_WEST
    Y = sdl.SDL_GAMEPAD_BUTTON_NORTH
    Back = sdl.SDL_GAMEPAD_BUTTON_BACK
    Guide = sdl.SDL_GAMEPAD_BUTTON_GUIDE
    Start = sdl.SDL_GAMEPAD_BUTTON_START
    LeftStick = sdl.SDL_GAMEPAD_BUTTON_LEFT_STICK
    RightStick = sdl.SDL_GAMEPAD_BUTTON_RIGHT_STICK
    LeftBumper = sdl.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER
    RightBumper = sdl.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER
    Up = sdl.SDL_GAMEPAD_BUTTON_DPAD_UP
    Down = sdl.SDL_GAMEPAD_BUTTON_DPAD_DOWN
    Left = sdl.SDL_GAMEPAD_BUTTON_DPAD_LEFT
    Right = sdl.SDL_GAMEPAD_BUTTON_DPAD_RIGHT
    Misc1 = sdl.SDL_GAMEPAD_BUTTON_MISC1
    RightPaddle1 = sdl.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1
    LeftPaddle1 = sdl.SDL_GAMEPAD_BUTTON_LEFT_PADDLE1
    RightPaddle2 = sdl.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2
    LeftPaddle2 = sdl.SDL_GAMEPAD_BUTTON_LEFT_PADDLE2
    Touchpad = sdl.SDL_GAMEPAD_BUTTON_TOUCHPAD

    inline __imply (lhsT rhsT)
        static-if (rhsT == sdl.GamepadButton)
            inline (self)
                bitcast ((storagecast self) as i32) sdl.GamepadButton
        else
            super-type.__imply lhsT rhsT

do
    let ControllerAxis ControllerButton
    locals;
