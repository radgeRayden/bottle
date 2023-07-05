bottle := __env.bottle
import C.stdio
using import print
using import String
from (import ..src.config) let if-module-enabled

@@ 'on bottle.load
fn ()
    renderer-info := (bottle.gpu.get-info)
    # FIXME: using this while compiler.Printer is broken for String
    print "bottle version:" (bottle.get-version)
    print renderer-info.APIString
    print renderer-info.GPUString
    ()

@@ 'on bottle.key-released
fn (key)
    using bottle.enums
    if (key == KeyboardKey.Escape)
        bottle.quit!;
    if (key == KeyboardKey.F11)
        bottle.window.toggle-fullscreen;

@@ 'on bottle.render
@@ if-module-enabled 'imgui
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
