let sdl = (import .FFI.sdl)

import .sysevents
import .window
import .gpu

fn run ()
    window.init;
    gpu.init;

    while (not (sysevents.really-quit?))
        sysevents.dispatch;
        gpu.present;
    ;

do
    let run
    locals;
