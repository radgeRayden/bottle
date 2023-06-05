bottle := __env.bottle

@@ 'on bottle.key-released
fn (key)
    using bottle.enums
    if (key == KeyboardKey.Escape)
        bottle.quit!;

do
    fn display-fps ()
        ig := import ..src.imgui
        using import glm
        flags := ig.GuiWindowFlags
        ig.SetNextWindowPos (ig.Vec2 10 10) ig.ImGuiCond_Always (ig.Vec2 0 0)
        ig.SetNextWindowSize (ig.Vec2 100 15) ig.ImGuiCond_Always
        ig.Begin "fps" null
            i32 (ig.ImGuiWindowFlags_NoDecoration | ig.ImGuiWindowFlags_NoBackground)
        fps := i32 (1 / (bottle.time.get-delta-time))
        ig.Text "FPS: %d" fps
        ig.End;
    local-scope;