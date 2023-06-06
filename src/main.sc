using import FunctionChain

import .sysevents
import .window
import .gpu
import .config
import .time
import .imgui

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
    time.init;
    imgui.init;
    load;

    USE_DT_ACCUMULATOR? := cfg.time.use-delta-accumulator?
    FIXED_TIMESTEP     := cfg.time.fixed-timestep
    local dt-accumulator : f64

    while (not (sysevents.really-quit?))
        sysevents.dispatch imgui.process-event

        time.step;
        dt := (time.get-delta-time)

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
            imgui.begin-frame;
            render render-pass
            imgui.render render-pass
            gpu.present render-pass
            imgui.end-frame;
        except (ex)
            using gpu.types
            match ex
            case GPUError.ObjectCreationFailed
                assert false "unhandled GPU Object creation failure"
            case GPUError.OutdatedSwapchain
                imgui.wgpu-reset;
                imgui.end-frame;
            default ()

    imgui.shutdown;
    window.shutdown;

sugar-if main-module?
    using import String

    name argc argv := (script-launch-args)
    let demo =
        if (argc > 0)
            String (argv @ 0)
        else
            S"gpu.hello-triangle"

    let module =
        try
            require-from (module-dir .. "/..") (.. ".demos." demo) __env
        except(ex)
            'dump ex
            error (.. "failed to load demo: " (demo as string))

    f := (compile (typify (module as Closure) i32 (@ rawstring))) as (@ (function void i32 (@ rawstring)))
    f argc argv
    0
else
    do
        let run load update fixed-update render configure
        locals;
