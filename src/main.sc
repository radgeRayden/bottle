import .audio .exceptions .filesystem .gpu .imgui .plonk .sysevents .time .window .callbacks
using import .context

@@ 'on callbacks.begin-frame
fn "main.begin-frame" ()
    imgui.begin-frame;
    plonk.begin-frame;

@@ 'on callbacks.end-frame
fn "main.end-frame" ()
    plonk.submit;
    imgui.end-frame;
    imgui.render;

fn run ()
    raising noreturn

    cfg := ((context-accessor 'config))
    callbacks.assign-callbacks;

    callbacks.configure cfg
    'apply-env-overrides cfg

    filesystem.init;
    window.init;
    gpu.init;
    time.init;
    audio.init;
    imgui.init;
    plonk.init;
    callbacks.load;

    USE_DT_ACCUMULATOR? := cfg.time.use-delta-accumulator?
    FIXED_TIMESTEP     := cfg.time.fixed-timestep
    local dt-accumulator : f64

    while (not (sysevents.really-quit?))
        sysevents.dispatch imgui.process-event

        time.step;
        dt := (time.get-delta-time)

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
    locals;
