using import FunctionChain

import .sysevents
import .window
import .gpu
import .config
import .timer

fnchain load
fnchain update
fnchain fixed-update
fnchain draw
fnchain configure

fn run ()
    cfg := config.istate-cfg
    configure cfg

    window.init;
    gpu.init;
    timer.init;
    load;

    USE_DT_ACCUMULATOR? := cfg.timer.use-delta-accumulator?
    FIXED_TIMESTEP     := cfg.timer.fixed-timestep
    local dt-accumulator : f64

    while (not (sysevents.really-quit?))
        sysevents.dispatch;

        timer.step;
        dt := (timer.get-delta-time)

        if USE_DT_ACCUMULATOR?
            dt-accumulator += dt

            while (dt-accumulator > FIXED_TIMESTEP)
                fixed-update FIXED_TIMESTEP
                dt-accumulator -= FIXED_TIMESTEP
            update dt
        else
            update dt

        try
            let render-pass = (gpu.begin-frame)
            # TODO: pass in dt remainder, after we adapt the timer module to be aware of it
            draw render-pass
            gpu.present render-pass
        else
            ()

do
    let run load update draw configure
    locals;
