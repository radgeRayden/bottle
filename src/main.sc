using import FunctionChain

let sdl = (import .FFI.sdl)

import .sysevents
import .window
import .gpu
import .config

fnchain load
fnchain update
fnchain draw
fnchain configure

fn run ()
    configure config.istate-cfg

    window.init;
    gpu.init;
    load;

    while (not (sysevents.really-quit?))
        sysevents.dispatch;
        update;

        try
            let render-pass = (gpu.begin-frame)
            draw render-pass
            gpu.present render-pass
        else
            return;

do
    let run load update draw configure
    locals;
