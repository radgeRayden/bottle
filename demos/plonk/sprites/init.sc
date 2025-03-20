using import glm Option struct print
import ...demo-common bottle

plonk  := bottle.plonk
using bottle.gpu.types

struct DrawState
    sprite : Texture
    sprite-frame : i32

global draw-state : (Option DrawState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "plonking sprites"
    cfg.platform.force-x11? = true

@@ 'on bottle.load
fn ()
    using bottle.enums
    using bottle.types

    try
        draw-state =
            DrawState
                sprite =
                    Texture (bottle.asset.load-image "assets/_Run.png")
    else ()

@@ 'on bottle.update
fn (dt)
    ctx := 'force-unwrap draw-state

    t := (bottle.time.get-time) * 11
    ctx.sprite-frame = (i32 (floor t)) % 10

@@ 'on bottle.render
fn ()
    from bottle.types let Quad
    ctx := 'force-unwrap draw-state

    using import itertools
    for x y in (dim 20 20)
        quad := Quad (vec2 (ctx.sprite-frame / 10) 0) (vec2 (1 / 10) 1)
        plonk.set-texture-filtering 'Nearest 'Nearest
        plonk.sprite ctx.sprite (vec2 (x * 20) (y * 20)) (vec2 100 100) 0:f32 quad
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
