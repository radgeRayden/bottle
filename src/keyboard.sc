import .enums sdl3

fn... down? (key : enums.KeyboardKey)
    kbstate := (sdl3.GetKeyboardState null)
    # FIXME: understand the modstate parameter
    bool (kbstate @ (sdl3.GetScancodeFromKey key null))

do
    let down?
    locals;
