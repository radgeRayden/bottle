using import FunctionChain

import .sysevents
import .window
import .gpu
import .config
import .timer

fnchain load
fnchain update
fnchain draw
fnchain configure

fn run ()
    configure config.istate-cfg

    window.init;
    gpu.init;
    timer.init;
    load;

    while (not (sysevents.really-quit?))
        sysevents.dispatch;

        timer.step;
        update (timer.get-delta-time)

        try
            let render-pass = (gpu.begin-frame)
            draw render-pass
            gpu.present render-pass
        else
            ()

do
    let run load update draw configure
    locals;
