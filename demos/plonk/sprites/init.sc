using import glm
using import Option
using import struct
using import print
import ...demo-common
import bottle

plonk  := bottle.plonk

struct DrawState
    sprite : plonk.TextureBinding
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
                sprite = plonk.TextureBinding (Texture (bottle.asset.load-image "assets/_Run.png")) (min-filter = 'Nearest)
    else ()

@@ 'on bottle.update
fn (dt)
    ctx := 'force-unwrap draw-state

    t := (bottle.time.get-time) * 11
    ctx.sprite-frame = (i32 (floor t)) % 10

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap draw-state

    using import itertools
    for x y in (dim 20 20)
        quad := plonk.Quad (vec2 (ctx.sprite-frame / 10) 0) (vec2 (1 / 10) 1)
        plonk.sprite ctx.sprite (vec2 (x * 20) (y * 20)) (vec2 100 100) 0:f32 quad
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
