import .audio .callbacks .filesystem .gpu .imgui .plonk .sysevents .time .window
using import .context

inline chain-callback (cb f...)
    cb := getattr callbacks cb
    'clear cb
    va-map
        inline (f)
            'append cb f
            ()
        f...

chain-callback 'load imgui.init plonk.init
chain-callback 'begin-frame imgui.begin-frame plonk.begin-frame
chain-callback 'end-frame plonk.submit imgui.end-frame imgui.render
chain-callback 'invalidate-frame imgui.reset-gpu-state

fn run ()
    raising noreturn

    cfg := ((context-accessor 'config))

    callbacks.configure cfg
    'apply-env-overrides cfg

    filesystem.init;
    window.init;
    gpu.init;
    time.init;
    audio.init;
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

            switch ex
            case 'ObjectCreationFailed
                # assert false "unhandled GPU Object creation failure"
                abort;
            case 'DiscardedFrame
                ()
            default ()

        time.sleep 1

    imgui.shutdown;
    window.shutdown;
    filesystem.shutdown;
    audio.shutdown;
    ()

do
    let run
    using callbacks
    locals;
