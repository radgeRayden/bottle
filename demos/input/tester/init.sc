using import glm
import bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.gpu.msaa-samples = 4

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    ww wh := (bottle.window.get-size)

    scenter := (vec2 (ww / 2) (wh / 2))

    # thumbsticks
    plonk.circle-line (scenter + (vec2 -160 -150)) 50:f32
    plonk.circle-line (scenter + (vec2 160 -150)) 50:f32

    # dpad
    dpad-center := scenter + (vec2 -300 20)
    plonk.rectangle (dpad-center + (vec2 -50 0)) (vec2 40 20)
    plonk.rectangle (dpad-center + (vec2 0 -50)) (vec2 40 20) (pi / 2)
    plonk.rectangle (dpad-center + (vec2 50 0)) (vec2 40 20) pi
    plonk.rectangle (dpad-center + (vec2 0 50)) (vec2 40 20) (3 * pi / 2)

    # buttons
    btn-center := scenter + (vec2 300 20)
    plonk.circle (btn-center + (vec2 -50 0)) 15 (vec4 0.1 0.58 0.77 1)
    plonk.circle (btn-center + (vec2 0 -50)) 15 (vec4 0.08 0.76 0.25 1)
    plonk.circle (btn-center + (vec2 50 0)) 15 (vec4 0.88 0.16 0.16 1)
    plonk.circle (btn-center + (vec2 0 50)) 15 (vec4 0.97 0.93 0.07 1)

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
