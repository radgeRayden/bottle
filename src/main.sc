using import FunctionChain

import .callbacks
import .config
import .filesystem
import .gpu
import .imgui
import .plonk
import .sysevents
import .time
import .window

inline chain-callback (cb f...)
    cb := getattr callbacks cb
    'clear cb
    va-map
        inline (f)
            'prepend cb f
            ()
        f...

chain-callback 'load plonk.init imgui.init
chain-callback 'begin-frame plonk.begin-frame imgui.begin-frame
chain-callback 'end-frame imgui.end-frame imgui.render plonk.submit

fn run ()
    raising noreturn

    cfg := config.istate-cfg
    callbacks.configure cfg

    filesystem.init;
    window.init;
    gpu.init;
    time.init;
    callbacks.load;

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
                callbacks.fixed-update FIXED_TIMESTEP
                dt-accumulator -= FIXED_TIMESTEP
            callbacks.update dt
        else
            callbacks.update dt

        try
            gpu.begin-frame;
            callbacks.begin-frame;
            callbacks.render;
            callbacks.end-frame;
            gpu.present;
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

do
    let run
    using callbacks
    locals;
