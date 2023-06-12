bottle := __env.bottle
plonk := bottle.plonk

using import enum
using import glm
using import Option
using import struct
import ..demo-common

struct GameState
    atlas : plonk.SpriteAtlas

global context : (Option GameState)

enum SpriteIndices plain
    SnakeHead
    SnakeBody
    SnakeCorner
    SnakeTail
    Fruit

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "snake"
    cfg.window.width = 800
    cfg.window.height = 600
    cfg.window.resizable? = false

@@ 'on bottle.load
fn ()
    try
        context =
            GameState
                atlas = plonk.SpriteAtlas (bottle.asset.load-image "snake.png") 5 1
    else ()

@@ 'on bottle.update
fn (dt)

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap context
    plonk.sprite ctx.atlas (vec2 0 0) (vec2 128 128) ('get-quad ctx.atlas SpriteIndices.Fruit)
    plonk.sprite ctx.atlas (vec2 128 0) (vec2 128 128) ('get-quad ctx.atlas SpriteIndices.SnakeHead)
    plonk.sprite ctx.atlas (vec2 256 0) (vec2 128 128) ('get-quad ctx.atlas SpriteIndices.SnakeBody)
    plonk.sprite ctx.atlas (vec2 384 0) (vec2 128 128) ('get-quad ctx.atlas SpriteIndices.SnakeTail)

    demo-common.display-fps;

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
