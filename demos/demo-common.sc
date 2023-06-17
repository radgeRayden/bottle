bottle := __env.bottle
import C.stdio
using import String

@@ 'on bottle.load
fn ()
    renderer-info := (bottle.gpu.get-info)
    # FIXME: using this while compiler.Printer is broken for String
    C.stdio.printf "bottle version: %s\n" ((bottle.get-version) as rawstring)
    C.stdio.printf "%s\n" ((copy (imply renderer-info.RendererString String)) as rawstring)
    ()

@@ 'on bottle.key-released
fn (key)
    using bottle.enums
    if (key == KeyboardKey.Escape)
        bottle.quit!;

@@ 'on bottle.render
fn display-fps ()
    ig := import ..src.imgui
    using import glm
    flags := ig.GuiWindowFlags
    ig.SetNextWindowPos (ig.Vec2 10 10) ig.ImGuiCond_Always (ig.Vec2 0 0)
    ig.SetNextWindowSize (ig.Vec2 150 40) ig.ImGuiCond_Always
    ig.Begin "fps" null
        i32 ig.ImGuiWindowFlags_NoDecoration
    ig.SetWindowFontScale 2
    fps := i32 (1 / (bottle.time.get-delta-time))
    ig.Text "FPS: %d" fps
    ig.End;
    ()

do
    local-scope;
