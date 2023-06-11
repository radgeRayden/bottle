using import glm
using import Option
using import struct
import ...demo-common

bottle := __env.bottle
plonk  := bottle.renderers.plonk

struct DrawState
    sprite : plonk.SpriteAtlas

global draw-state : (Option DrawState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "plonking sprites"

@@ 'on bottle.load
fn ()
    using bottle.enums

    try
        plonk.init;

        draw-state =
            DrawState
                sprite = plonk.SpriteAtlas (bottle.asset.load-image "_Run.png") 10 1
    else ()

@@ 'on bottle.update
fn (dt)
    ctx := 'force-unwrap draw-state

    t := (bottle.time.get-time) * 11
    'set-frame ctx.sprite (i32 (floor t))

@@ 'on bottle.render
fn (render-pass)
    ctx := 'force-unwrap draw-state
    plonk.begin-frame;

    using import itertools
    for x y in (dim 20 20)
        plonk.sprite ctx.sprite (vec2 (x * 20) (y * 20)) (vec2 100 100) (vec4 1)

    plonk.submit;
    demo-common.display-fps;

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
