import sdl

fn down? (key)
    kbstate := (sdl.GetKeyboardState null)
    bool (kbstate @ (sdl.GetScancodeFromKey key))

do
    let down?
    locals;
