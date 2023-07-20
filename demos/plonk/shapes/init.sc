using import Array
using import glm
import ...demo-common
import bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "geometric shapes"
    cfg.window.fullscreen? = false
    cfg.gpu.msaa-samples = 4

global rotation : f32
global line-width : f32
global line-vertices : (Array vec2)
global line-vertices2 : (Array vec2)

@@ 'on bottle.update
fn (dt)
    w h := (bottle.window.get-size)
    center := vec2 (w / 2) (h / 2)
    segments := 2000

    t := (f32 (bottle.time.get-time)) / 3
    rotation = t * 5

    inline get-point (k)
        k as:= f32
        segments as:= f32
        radius := (k / segments) * 300
        theta := (pi / (segments / 10)) * k + rotation
        center + (vec2 (cos theta) (sin theta)) * radius

    'clear line-vertices

    progress := usize (bottle.math.ceil ((f32 segments) * t))
    for i in (range (progress % segments))
        k := i * 2
        'append line-vertices (get-point k)
        'append line-vertices (get-point (k + 1))

    'clear line-vertices2
    lineh := h / 8
    linew := w / 3
    segments := 10
    sstep := abs ((linew / segments) * (sin (t * 5)))
    for i in (range segments)
        i as:= f32
        rmargin := (f32 w) - 50
        vmargin := 50
        'append line-vertices2 (vec2 (rmargin - (sstep * i)) (vmargin + lineh))
        'append line-vertices2 (vec2 (rmargin - (sstep * (i + 0.5))) vmargin)

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    from plonk let LineJoinKind LineCapKind
    plonk.rectangle (vec2 200 450) (vec2 200 100) rotation (vec4 1 0 1 1)
    plonk.circle (vec2 150 200) 100 (color = (vec4 0 1 0 1))
    plonk.polygon (vec2 150 200) 3 100 rotation (vec4 0 0.5 0.5 1)
    plonk.line line-vertices 15:f32 (vec4 1 0.5 0.7 1) LineJoinKind.Round LineCapKind.Round
    plonk.line line-vertices 7:f32 (vec4 0 0.5 0.7 1) LineJoinKind.Round LineCapKind.Round
    plonk.polygon (vec2 450 200) 5 100 rotation

    plonk.line line-vertices2 35:f32 (vec4 1 0.5 0.7 1) LineJoinKind.Bevel LineCapKind.Square
    plonk.line line-vertices2 30:f32 (vec4 0 0.5 0.7 1) LineJoinKind.Bevel LineCapKind.Square

    wsize := vec2 (bottle.window.get-size)
    plonk.rectangle-line (wsize - (vec2 250)) (vec2 100 300) rotation
    plonk.polygon-line (vec2 200 (wsize.y - 250)) 8 200 rotation
    plonk.circle-line (vec2 200 (wsize.y - 250)) 200

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
