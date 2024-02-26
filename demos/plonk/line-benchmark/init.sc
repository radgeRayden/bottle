using import glm
using import itertools

import bottle
plonk := bottle.plonk

import ...demo-common

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "many, many lines"
    cfg.window.fullscreen? = true
    cfg.gpu.msaa? = true

LINES-X LINES-Y := 250, 250
@@ 'on bottle.render
fn ()
    ww wh := (bottle.window.get-size)
    t := (bottle.time.get-time)
    for x y in (dim LINES-X LINES-Y)
        spacing-x spacing-y := ww / LINES-X, wh / LINES-Y
        x0 y0 := ((f32 x) + 0.5) * spacing-x, ((f32 y) + 0.5) * spacing-y
        start := vec2 x0 y0
        end := start + (vec2 (cos t) (sin t)) * 15
        local vertices = (arrayof vec2 start end)
        plonk.line vertices 2.5:f32

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
