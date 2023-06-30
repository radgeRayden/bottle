using import Array
using import glm
import ...demo-common
bottle := __env.bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "geometric shapes"
    cfg.window.fullscreen? = true

global rotation : f32
global line-width : f32
global line-vertices : (Array vec2)

@@ 'on bottle.update
fn (dt)
    rotation += (f32 dt) * 4

    w h := (bottle.window.get-size)
    'clear line-vertices
    count := 6
    time := (bottle.time.get-time)
    scaling := ((sin (f32 time)) + 1) / 2
    spacing := (w / count) * scaling * 0.9
    margin  := ((f32 w) - spacing * count) / 2 + spacing / 4

    for i in (range count)
        'append line-vertices
            vec2 (margin + (spacing * (f32 i))) 250
        'append line-vertices
            vec2 (margin + (spacing * (f32 i)) + (spacing / 2)) (h - 250)

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    plonk.rectangle (vec2 50 100) (vec2 200) rotation (vec4 1 0 1 1)
    plonk.circle (vec2 150 200) 100 (color = (vec4 0 1 0 1))
    plonk.polygon (vec2 150 200) 3 100 rotation (vec4 0 0.5 0.5 1)
    plonk.line line-vertices 75:f32 (vec4 1 0.5 0.7 1)
    for i in (range ((countof line-vertices) // 2))
        idx := i * 2
        (line-vertices @ idx) . y += 12.5
        (line-vertices @ (idx + 1)) . y -= 12.5
    plonk.line line-vertices 50:f32 (vec4 0 0.5 0.7 1)
    plonk.polygon (vec2 500 200) 5 100 rotation


sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
