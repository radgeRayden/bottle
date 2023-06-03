using import FunctionChain

import .sysevents
import .window
import .gpu
import .config
import .timer

fnchain load
fnchain update
fnchain fixed-update
fnchain render
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
            render-pass := (gpu.begin-frame)
            # TODO: pass in dt remainder, after we adapt the timer module to be aware of it
            render render-pass
            gpu.present render-pass
        except (ex)
            using gpu.types
            if (ex == GPUError.ObjectCreationFailed)
                assert false "unhandled GPU Object creation failure"

    window.shutdown;

sugar-if main-module?
    using import String

    name argc argv := (script-launch-args)
    let demo =
        if (argc > 0)
            String (argv @ 1)
        else
            S"gpu.hello-triangle"

    let module =
        try
            require-from (module-dir .. "/..") (.. ".demos." demo) __env
        else
            error (string (.. "unknown demo:" demo))

    f := (compile (typify (module as Closure) i32 (@ rawstring))) as (@ (function void i32 (@ rawstring)))
    f argc argv
    0
else
    do
        let run load update fixed-update render configure
        locals;
