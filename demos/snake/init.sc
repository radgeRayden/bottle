using import Array
using import enum
using import glm
using import itertools
using import Option
using import String
using import struct
import ..demo-common

import bottle
plonk := bottle.plonk
audio := bottle.audio

TILE-SIZE := 32
SCREEN-WIDTH SCREEN-HEIGHT := 800, 608
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

struct SnakeSegment plain
    position : ivec2
    direction : Direction
    corner? : bool

struct GameResources
    atlas : plonk.SpriteAtlas
    font-atlas : plonk.SpriteAtlas

struct GameState
    score : i32
    movement-timer : f64
    snake-speed : i32 = 7
    snake-length : i32 = 3
    snake-segments : (Array SnakeSegment)
    snake-direction = Direction.Left
    next-snake-direction = Direction.Left
    fruit-position : ivec2
    playing-field : (Array ObjectType)

inline idx->xy (idx)
    assert (idx > 0 and idx < TILES-W * TILES-H)
    x y := idx % TILES-W, idx // TILES-W

inline xy->idx (x y)
    assert (x >= 0 and x < TILES-W and y >= 0 and y < TILES-H)
    y * TILES-W + x

global resources : (Option GameResources)
global game-state : GameState
global rng : bottle.random.RNG 0

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "snake"
    cfg.window.width = SCREEN-WIDTH
    cfg.window.height = SCREEN-HEIGHT
    cfg.window.resizable? = false

fn play-sfx (name)
    audio.play-one-shot (.. "/assets/" name ".wav")

fn spawn-snake ()
    for x in (range game-state.snake-length)
        segment := SnakeSegment (ivec2 ((TILES-W // 2) + x) (TILES-H // 2)) Direction.Left false
        'append game-state.snake-segments segment
        game-state.playing-field @ (xy->idx (unpack segment.position)) = ObjectType.Snake

fn spawn-fruit ()
    local available-spots : (Array ivec2)
    for x y in (dim TILES-W TILES-H)
        thing := game-state.playing-field @ (xy->idx x y)
        if (thing == ObjectType.Empty)
            'append available-spots (ivec2 x y)

    if ((countof available-spots) > 0)
        position := available-spots @ (rng (countof available-spots))
        game-state.fruit-position = position
        game-state.playing-field @ (xy->idx (unpack game-state.fruit-position)) = ObjectType.Fruit

fn setup-game ()
    game-state = (GameState)
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

fn update-snake ()
    game-state.movement-timer = 0
    segments := game-state.snake-segments
    dir next-dir := game-state.snake-direction, game-state.next-snake-direction
    switch next-dir
    case Direction.Up
        if (dir == Direction.Down) (next-dir = dir)
    case Direction.Right
        if (dir == Direction.Left) (next-dir = dir)
    case Direction.Left
        if (dir == Direction.Right) (next-dir = dir)
    case Direction.Down
        if (dir == Direction.Up) (next-dir = dir)
    default ()

    tail := deref ('last segments)
    game-state.playing-field @ (xy->idx (unpack tail.position)) = ObjectType.Empty
    for i segment in (zip (rrange (countof segments)) ('reverse segments))
        if (i == 0)
            switch next-dir
            case Direction.Up
                segment.position += ivec2 0 1
            case Direction.Right
                segment.position += ivec2 1 0
            case Direction.Down
                segment.position += ivec2 0 -1
            case Direction.Left
                segment.position += ivec2 -1 0
            default ()

            segment.direction = next-dir
        else
            prev-segment := segments @ (i - 1)
            segment = prev-segment

    if (dir != next-dir)
        (segments @ 1) . corner? = true
        (segments @ 1) . direction = next-dir
    dir = next-dir

    thing-at := deref (game-state.playing-field @ (xy->idx (unpack ((segments @ 0) . position))))

    ate-fruit? := thing-at == ObjectType.Fruit
    if ate-fruit?
        game-state.score += 1
        'append segments tail

    for segment in segments
        game-state.playing-field @ (xy->idx (unpack segment.position)) = ObjectType.Snake

    if ate-fruit?
        play-sfx "bubble1"
        spawn-fruit;

    if (thing-at == ObjectType.Snake or thing-at == ObjectType.Wall)
        game-state.playing-field @ (xy->idx (unpack tail.position)) = ObjectType.Snake
        play-sfx "game_over"
        setup-game;

@@ 'on bottle.load
fn ()
    try
        using bottle.types
        using bottle.enums

        font-data := 'read-all-bytes (FileStream "assets/monogram.ttf" FileMode.Read)
        font := bottle.font.Font font-data 39

        resources =
            GameResources
                atlas = plonk.SpriteAtlas (bottle.asset.load-image "assets/snake.png") 6 1
                font-atlas = plonk.SpriteAtlas ('pack-atlas font char" " char"~") 1 95
    else ()

    setup-game;

global debug-mode? : bool
global debug-flip? : bool
@@ 'on bottle.key-pressed
fn (key)
    cur-dir next-dir := game-state.snake-direction, game-state.next-snake-direction
    inline update-dir (dir)
        if (dir != next-dir)
            next-dir = dir
            update-snake;

    using bottle.enums
    switch key
    case KeyboardKey.Up
        update-dir Direction.Up
    case KeyboardKey.Right
        update-dir Direction.Right
    case KeyboardKey.Down
        update-dir Direction.Down
    case KeyboardKey.Left
        update-dir Direction.Left
    case KeyboardKey.Space
        if debug-mode?
            update-snake;
    case KeyboardKey.x
        if debug-mode?
            debug-flip? = not debug-flip?
    case KeyboardKey.d
        debug-mode? = not debug-mode?
    default ()

@@ 'on bottle.update
fn (dt)
    using bottle.enums

    game-state.movement-timer += dt
    let speed =
        if ((not debug-mode?) and (bottle.keyboard.down? KeyboardKey.Space))
            game-state.snake-speed * 3
        else
            deref game-state.snake-speed

    turn-duration := 1 / speed

    if (game-state.movement-timer >= turn-duration)
        if (not debug-mode?)
            update-snake;

@@ 'on bottle.render
fn ()
    ctx := 'force-unwrap resources
    field := game-state.playing-field

    inline draw-tile (position tile rotation flip...)
        fliph? flipv? := (va-option fliph? flip... false), (va-option flipv? flip... false)
        tile-size := (vec2 TILE-SIZE)
        drawpos := (vec2 position) * tile-size + (tile-size / 2)
        plonk.sprite ctx.atlas drawpos tile-size (rotation as f32) ('get-quad ctx.atlas tile)
            fliph? = fliph?
            flipv? = flipv?

    for x y in (dim TILES-W TILES-H)
        obj := (field @ (xy->idx x y))
        position := ivec2 x y

        switch obj
        case ObjectType.Wall
            draw-tile position SpriteIndices.Wall 0
        case ObjectType.Fruit
            draw-tile position SpriteIndices.Fruit 0
        default ()

    segments := game-state.snake-segments
    for i segment in (enumerate segments)
        let rotation =
            switch segment.direction
            case Direction.Left
                0:f32
            case Direction.Down
                pi:f32 / 2
            case Direction.Right
                pi:f32
            case Direction.Up
                -pi:f32 / 2
            default (unreachable)

        if (i == 0)
            draw-tile segment.position SpriteIndices.SnakeHead rotation
        elseif (i == ((countof segments) - 1))
            draw-tile segment.position SpriteIndices.SnakeTail rotation
        else
            tile := segment.corner? SpriteIndices.SnakeCorner SpriteIndices.SnakeBody
            let fliph? flipv? =
                if ((not debug-flip?) and segment.corner?)
                    next-segment := segments @ (i + 1)
                    dir next-dir := segment.direction, next-segment.direction
                    if (dir == Direction.Up and next-dir == Direction.Left)
                        _ false true
                    elseif (dir == Direction.Right and next-dir == Direction.Up)
                        _ false true
                    elseif (dir == Direction.Down and next-dir == Direction.Right)
                        _ false true
                    elseif (dir == Direction.Left and next-dir == Direction.Down)
                        _ false true
                    else
                        _ false false
                else (_ false false)

            draw-tile segment.position tile rotation (fliph? = fliph?) (flipv? = flipv?)

    using import radl.strfmt
    for i c in (enumerate f"score: ${game-state.score}")
        plonk.sprite ctx.font-atlas (vec2 (i * 15) 0) (vec2 15 31) 0:f32 ('get-quad ctx.font-atlas (c - char" "))
    ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
