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
        plonk.init
            width = 640
            height = 480
            filtering = FilterMode.Nearest
    else ()

    # draw-state =
    #     DrawState
    #         sprite = plonk.SpriteAtlas (bottle.asset.load-image "_Run.png") 10 1

@@ 'on bottle.update
fn (dt)
    # ctx := 'force-unwrap draw-state

    # t := (bottle.time.get-time) * 5
    # 'set-frame ctx.sprite (i32 (floor t))

@@ 'on bottle.render
fn (render-pass)
    # ctx := 'force-unwrap draw-state
    plonk.begin-frame;
    # plonk.sprite ctx.sprite 10 10
    plonk.submit render-pass

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
