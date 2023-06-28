using import Array
using import glm
import ...demo-common
bottle := __env.bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "geometric shapes"

global rotation : f32
global line-vertices : (Array vec2)

@@ 'on bottle.update
fn (dt)
    rotation += (f32 dt) * 4

    w h := (bottle.window.get-size)
    inline calculate-height (i)
        (((sin (((i / w) * pi * 10) + (f32 (bottle.time.get-time)))) * (f32 h)) / 2) + ((f32 h) / 2)

    'clear line-vertices
    for i in (range w)
        'append line-vertices (vec2 i (calculate-height i))
        'append line-vertices (vec2 (i + 1) (calculate-height (i + 1)))

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    plonk.line line-vertices
    plonk.rectangle (vec2 50 100) (vec2 200) rotation (vec4 1 0 1 1)
    plonk.circle (vec2 150 200) 100 (color = (vec4 0 1 0 1))
    plonk.polygon (vec2 150 200) 3 100 rotation (vec4 0 0.5 0.5 1)
    plonk.polygon (vec2 500 200) 5 100 rotation

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
