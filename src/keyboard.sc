import .enums sdl

fn... down? (key : enums.KeyboardKey)
    kbstate := (sdl.GetKeyboardState null)
    bool (kbstate @ (sdl.GetScancodeFromKey key))

do
    let down?
    locals;
