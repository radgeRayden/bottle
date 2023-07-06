using import glm
using import Option
using import struct
import ...demo-common

bottle := __env.bottle
plonk  := bottle.plonk

struct DrawState
    sprite : plonk.SpriteAtlas
    sprite-frame : i32

global draw-state : (Option DrawState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "plonking sprites"

@@ 'on bottle.load
fn ()
    using bottle.enums

    try
        draw-state =
            DrawState
                sprite = plonk.SpriteAtlas (bottle.asset.load-image "assets/_Run.png") 10 1
    else ()

@@ 'on bottle.update
fn (dt)
    ctx := 'force-unwrap draw-state

    t := (bottle.time.get-time) * 11
    ctx.sprite-frame = (i32 (floor t))

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap draw-state

    using import itertools
    for x y in (dim 20 20)
        plonk.sprite ctx.sprite (vec2 (x * 20) (y * 20)) (vec2 100 100) 0:f32 ('get-quad ctx.sprite ctx.sprite-frame)
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
