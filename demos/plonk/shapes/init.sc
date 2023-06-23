using import glm
import ...demo-common
bottle := __env.bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "geometric shapes"

@@ 'on bottle.update
fn (dt)

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    # plonk.rectangle (vec2) (vec2 100)
    plonk.circle (vec2 100) 100

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
