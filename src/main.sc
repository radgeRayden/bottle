using import FunctionChain

let sdl = (import .FFI.sdl)

import .sysevents
import .window
import .gpu

fnchain load
fnchain update
fnchain draw

fn run ()
    window.init;
    gpu.init;
    load;

    while (not (sysevents.really-quit?))
        sysevents.dispatch;
        update;
        gpu.present draw
    ;

do
    let run load update draw
    locals;
