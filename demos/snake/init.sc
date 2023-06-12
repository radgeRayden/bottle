bottle := __env.bottle
plonk := bottle.plonk

using import Array
using import enum
using import glm
using import Option
using import struct
import ..demo-common

TILE-SIZE := 64
SCREEN-WIDTH SCREEN-HEIGHT := 800, 600
TILES-W TILES-H := SCREEN-WIDTH // TILE-SIZE, SCREEN-HEIGHT // TILE-SIZE

enum SpriteIndices plain
    SnakeHead
    SnakeBody
    SnakeCorner
    SnakeTail
    Fruit
    Wall

enum ObjectType plain
    Snake
    Fruit
    Empty
    Wall

    inline __typecall (cls)
        this-type.Empty

enum Direction plain
    Up
    Right
    Down
    Left

struct DrawState
    atlas : plonk.SpriteAtlas

struct GameState
    score : i32
    movement-timer : f64
    snake-speed : i32 = 2
    snake-length : i32 = 3
    snake-segments : (Array ivec2)
    snake-direction : Direction = Direction.Left
    fruit-position : ivec2
    playing-field : (Array ObjectType)

inline idx->xy (idx)
    assert (idx > 0 and idx < TILES-W * TILES-H)
    x y := idx % TILES-W, idx // TILES-W

inline xy->idx (x y)
    assert (x >= 0 and x < TILES-W and y >= 0 and y < TILES-H)
    y * TILES-W + x

global draw-context : (Option DrawState)
global game-state : GameState
global rng : bottle.random.RNG 0

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "snake"
    cfg.window.width = SCREEN-WIDTH
    cfg.window.height = SCREEN-HEIGHT
    cfg.window.resizable? = false

fn spawn-snake ()
    for x in (range game-state.snake-length)
        segment := ivec2 ((TILES-W // 2) + x) (TILES-H // 2)
        'append game-state.snake-segments segment
        game-state.playing-field @ (xy->idx (unpack segment)) = ObjectType.Snake

fn spawn-fruit ()
    loop (tries = 0)
        try-position := ivec2 (rng 1 (TILES-W - 2)) (rng 1 (TILES-H - 2))
        if (game-state.playing-field @ (xy->idx (unpack try-position)) == ObjectType.Empty)
            game-state.fruit-position = try-position
            game-state.playing-field @ (xy->idx (unpack game-state.fruit-position)) = ObjectType.Fruit
            break true

        if (tries > 100)
            break false # just so we don't hard lock the game, we can try again next frame.
        tries + 1

fn update-snake (dir)
    segments := game-state.snake-segments

    tail := deref ('last segments)
    game-state.playing-field @ (xy->idx (unpack tail)) = ObjectType.Empty
    for i segment in (zip (rrange (countof segments)) ('reverse segments))
        if (i == 0)
            switch dir
            case Direction.Up
                segment += ivec2 0 1
            case Direction.Right
                segment += ivec2 1 0
            case Direction.Down
                segment += ivec2 0 -1
            case Direction.Left
                segment += ivec2 -1 0
            default ()
        else
            segment = (segments @ (i - 1))

    thing-at := deref (game-state.playing-field @ (xy->idx (unpack (segments @ 0))))
    for segment in segments
        game-state.playing-field @ (xy->idx (unpack segment)) = ObjectType.Snake

    if (thing-at == ObjectType.Fruit)
        game-state.score += 1
        'append segments tail
        game-state.playing-field @ (xy->idx (unpack tail)) = ObjectType.Snake
        spawn-fruit;
    elseif (thing-at == ObjectType.Snake or thing-at == ObjectType.Wall)
        print "game-over"

@@ 'on bottle.load
fn ()
    try
        draw-context =
            DrawState
                atlas = plonk.SpriteAtlas (bottle.asset.load-image "snake.png") 6 1
    else ()

    field := game-state.playing-field
    'resize field (TILES-W * TILES-H)

    for i in (range TILES-W)
        field @ (xy->idx i 0) = ObjectType.Wall
        field @ (xy->idx i (TILES-H - 1)) = ObjectType.Wall
    for i in (range TILES-H)
        field @ (xy->idx 0 i) = ObjectType.Wall
        field @ (xy->idx (TILES-W - 1) i) = ObjectType.Wall

    spawn-snake;
    spawn-fruit;

@@ 'on bottle.key-pressed
fn (key)
    using bottle.enums
    switch key
    case KeyboardKey.Up
        game-state.snake-direction = Direction.Up
    case KeyboardKey.Right
        game-state.snake-direction = Direction.Right
    case KeyboardKey.Down
        game-state.snake-direction = Direction.Down
    case KeyboardKey.Left
        game-state.snake-direction = Direction.Left
    default ()

@@ 'on bottle.update
fn (dt)
    game-state.movement-timer += dt
    turn-duration := 1 / game-state.snake-speed

    if (game-state.movement-timer >= turn-duration)
        game-state.movement-timer -= turn-duration
        update-snake game-state.snake-direction

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap draw-context
    field := game-state.playing-field

    inline draw-tile (x y tile)
        plonk.sprite ctx.atlas (vec2 (x * TILE-SIZE) (y * TILE-SIZE)) (vec2 TILE-SIZE) ('get-quad ctx.atlas tile)

    using import itertools
    for x y in (dim TILES-W TILES-H)
        obj := (field @ (xy->idx x y))
        switch obj
        case ObjectType.Wall
            draw-tile x y SpriteIndices.Wall
        case ObjectType.Fruit
            draw-tile x y SpriteIndices.Fruit
        default ()

    segments := game-state.snake-segments
    for i segment in (enumerate segments)
        if (i == 0)
            draw-tile segment.x segment.y SpriteIndices.SnakeHead
        elseif (i == ((countof segments) - 1))
            draw-tile segment.x segment.y SpriteIndices.SnakeTail
        else
            draw-tile segment.x segment.y SpriteIndices.SnakeBody

    demo-common.display-fps;

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
