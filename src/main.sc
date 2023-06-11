using import FunctionChain

import .config
import .filesystem
import .gpu
import .imgui
import .sysevents
import .time
import .window
import .callbacks

fn run ()
    using callbacks

    cfg := config.istate-cfg
    configure cfg

    filesystem.init;
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
            using import .exceptions
            match ex
            case GPUError.ObjectCreationFailed
                assert false "unhandled GPU Object creation failure"
            case GPUError.OutdatedSwapchain
                imgui.wgpu-reset;
                imgui.end-frame;
            default ()

    imgui.shutdown;
    window.shutdown;
    filesystem.shutdown;
    ()

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
            C := (include "unistd.h") . extern

            import-string := .. "..demos." demo
            module := require-from module-dir import-string __env
            # set cwd so filesystem.mount points to the correct place
            path := dots-to-slashes (import-string as string)
            assert ((C.chdir (module-dir .. path)) == 0)
            module
        except(ex)
            'dump ex
            error (.. "failed to load demo: " (demo as string))

    f := (compile (typify (module as Closure) i32 (@ rawstring))) as (@ (function i32 i32 (@ rawstring)))
    f argc argv
    0
else
    do
        let run
        using callbacks
        locals;
