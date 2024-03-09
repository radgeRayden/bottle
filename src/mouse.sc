using import .enums
sdl := import sdl3

fn down? (button)
    state := (sdl.GetMouseState null null)
    bool
        state & (1:u32 << ((button - 1) as u32))

fn get-position ()
    local x : i32
    local y : i32
    state := (sdl.GetMouseState &x &y)
    _ x y

do
    let down? get-position
    locals;
