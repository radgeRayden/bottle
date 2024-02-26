using import Array glm Option String struct radl.strfmt
import bottle sdl
using import print

@@ 'on bottle.configure
fn (cfg)
    cfg.gpu.msaa? = true
    cfg.window.width = 1600
    cfg.window.height = 900

struct AppContext
    controllers : (Array (mutable@ sdl.GameController))
    selected-controller : (mutable@ sdl.GameController)

global ctx : AppContext

@@ 'on bottle.load
fn ()

@@ 'on bottle.controller-added
fn (id)
    'append ctx.controllers (sdl.GameControllerOpen id)

@@ 'on bottle.controller-removed
fn (id)


fn render-UI ()
    ig := bottle.imgui
    WF := ig.WindowFlags
    ww wh := (bottle.window.get-size)

    ig.SetNextWindowSize (ig.Vec2 210 (f32 wh)) ig.Cond.Always
    ig.SetNextWindowPos (ig.Vec2) ig.Cond.Always (ig.Vec2)
    ig.Begin "Controller Selector" null
        | WF.NoResize WF.NoScrollbar WF.NoCollapse WF.NoTitleBar
    ig.Text "Controller"
    ig.BeginListBox "##" (ig.Vec2 200 400)
    for idx id in (enumerate ctx.controllers)
        is-selected? := id == ctx.selected-controller

        name := sdl.GameControllerName id
        name := (name == null) &"unnamed" name
        if (ig.Selectable_Bool f"${name}##${idx}" is-selected? 0 (ig.Vec2))
            ctx.selected-controller = id

    ig.EndListBox;
    ig.End;

fn... button-down? (button : bottle.enums.ControllerButton)
    bool (sdl.GameControllerGetButton ctx.selected-controller button)

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    ww wh := (bottle.window.get-size)

    scenter := (vec2 (ww / 2) (wh / 2))

    # thumbsticks
    plonk.circle-line (scenter + (vec2 -160 -150)) 50:f32
    plonk.circle-line (scenter + (vec2 160 -150)) 50:f32

    # dpad
    dpad-center := scenter + (vec2 -300 20)
    plonk.rectangle (dpad-center + (vec2 -50 0)) (vec2 40 20)
    plonk.rectangle (dpad-center + (vec2 0 -50)) (vec2 40 20) (pi / 2)
    plonk.rectangle (dpad-center + (vec2 50 0)) (vec2 40 20) pi
    plonk.rectangle (dpad-center + (vec2 0 50)) (vec2 40 20) (3 * pi / 2)

    # buttons
    btn-center := scenter + (vec2 300 20)

    if (ctx.selected-controller != null)
        plonk.circle-line (btn-center + (vec2 -50 0)) 15 (vec4 0.1 0.58 0.77 1)
        if (button-down? 'X)
            plonk.circle (btn-center + (vec2 -50 0)) 15 (vec4 0.1 0.58 0.77 1)

        plonk.circle-line (btn-center + (vec2 0 -50)) 15 (vec4 0.08 0.76 0.25 1)
        if (button-down? 'A)
            plonk.circle (btn-center + (vec2 0 -50)) 15 (vec4 0.08 0.76 0.25 1)

        plonk.circle-line (btn-center + (vec2 50 0)) 15 (vec4 0.88 0.16 0.16 1)
        if (button-down? 'B)
            plonk.circle (btn-center + (vec2 50 0)) 15 (vec4 0.88 0.16 0.16 1)

        plonk.circle-line (btn-center + (vec2 0 50)) 15 (vec4 0.97 0.93 0.07 1)
        if (button-down? 'Y)
            plonk.circle (btn-center + (vec2 0 50)) 15 (vec4 0.97 0.93 0.07 1)

    render-UI;

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
