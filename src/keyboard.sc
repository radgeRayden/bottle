import .enums sdl3

fn... down? (key : enums.KeyboardKey)
    kbstate := (sdl3.GetKeyboardState null)
    bool (kbstate @ (sdl3.GetScancodeFromKey key))

do
    let down?
    locals;
