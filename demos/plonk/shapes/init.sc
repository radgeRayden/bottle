using import Array
using import glm
import ...demo-common
bottle := __env.bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "geometric shapes"
    cfg.window.fullscreen? = false
    cfg.gpu.msaa-samples = 4

global rotation : f32
global line-width : f32
global line-vertices : (Array vec2)

@@ 'on bottle.update
fn (dt)
    rotation += (f32 dt)

    w h := (bottle.window.get-size)
    center := vec2 (w / 2) (h / 2)
    segments := 2000
    inline get-point (k)
        k as:= f32
        radius := (k / segments) * 300
        theta := (pi / (segments / 10)) * k + rotation
        center + (vec2 (cos theta) (sin theta)) * radius

    'clear line-vertices

    t := (f32 (bottle.time.get-time)) / 3
    progress := usize (bottle.math.ceil (segments * t))
    for i in (range (min progress 5000:usize))
        k := i * 2
        'append line-vertices (get-point k)
        'append line-vertices (get-point (k + 1))

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    from plonk let LineJoinKind LineCapKind
    plonk.rectangle (vec2 50 100) (vec2 200) rotation (vec4 1 0 1 1)
    plonk.circle (vec2 150 200) 100 (color = (vec4 0 1 0 1))
    plonk.polygon (vec2 150 200) 3 100 rotation (vec4 0 0.5 0.5 1)
    plonk.line line-vertices 15:f32 (vec4 1 0.5 0.7 1) LineJoinKind.Round LineCapKind.Round
    plonk.line line-vertices 7:f32 (vec4 0 0.5 0.7 1) LineJoinKind.Round LineCapKind.Round
    plonk.polygon (vec2 500 200) 5 100 rotation

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
