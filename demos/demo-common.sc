bottle := __env.bottle

@@ 'on bottle.load
fn ()
    renderer-info := (bottle.gpu.get-info)
    print "bottle version:" (bottle.get-version)
    print renderer-info.RendererString

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

do
    local-scope;
