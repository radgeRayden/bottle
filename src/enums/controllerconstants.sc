using import enum
sdl := import sdl3

enum ControllerAxis : u8
    LeftX = sdl.GamepadAxis.LEFTX
    LeftY = sdl.GamepadAxis.LEFTY
    RightX = sdl.GamepadAxis.RIGHTX
    RightY = sdl.GamepadAxis.RIGHTY
    LeftTrigger = sdl.GamepadAxis.LEFT_TRIGGER
    RightTrigger = sdl.GamepadAxis.RIGHT_TRIGGER

    inline __imply (lhsT rhsT)
        static-if (rhsT == sdl.GamepadAxis)
            inline (self)
                bitcast ((storagecast self) as i32) sdl.GamepadAxis
        else
            super-type.__imply lhsT rhsT

enum ControllerButton : u8
    A = sdl.GamepadButton.SOUTH
    B = sdl.GamepadButton.EAST
    X = sdl.GamepadButton.WEST
    Y = sdl.GamepadButton.NORTH
    Back = sdl.GamepadButton.BACK
    Guide = sdl.GamepadButton.GUIDE
    Start = sdl.GamepadButton.START
    LeftStick = sdl.GamepadButton.LEFT_STICK
    RightStick = sdl.GamepadButton.RIGHT_STICK
    LeftBumper = sdl.GamepadButton.LEFT_SHOULDER
    RightBumper = sdl.GamepadButton.RIGHT_SHOULDER
    Up = sdl.GamepadButton.DPAD_UP
    Down = sdl.GamepadButton.DPAD_DOWN
    Left = sdl.GamepadButton.DPAD_LEFT
    Right = sdl.GamepadButton.DPAD_RIGHT
    Misc1 = sdl.GamepadButton.MISC1
    RightPaddle1 = sdl.GamepadButton.RIGHT_PADDLE1
    LeftPaddle1 = sdl.GamepadButton.LEFT_PADDLE1
    RightPaddle2 = sdl.GamepadButton.RIGHT_PADDLE2
    LeftPaddle2 = sdl.GamepadButton.LEFT_PADDLE2
    Touchpad = sdl.GamepadButton.TOUCHPAD

    inline __imply (lhsT rhsT)
        static-if (rhsT == sdl.GamepadButton)
            inline (self)
                bitcast ((storagecast self) as i32) sdl.GamepadButton
        else
            super-type.__imply lhsT rhsT

do
    let ControllerAxis ControllerButton
    locals;
