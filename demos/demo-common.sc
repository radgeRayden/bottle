import C.stdio
using import print radl.ext String
from (import ..src.context) let if-module-enabled

import bottle

@@ 'on bottle.configure
fn (cfg)
    static-if __env.use-hardcoded-root?
        cfg.filesystem.root = String module-dir

@@ 'on bottle.load
fn "demo-load" ()
    # renderer-info := (bottle.gpu.get-info)
    # print "bottle version:" (bottle.get-version)
    # print renderer-info.BackendString
    # print renderer-info.GPUString
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
    ig.SetNextWindowPos (ig.Vec2 10 10) ig.Cond.Always (ig.Vec2 0 0)
    ig.SetNextWindowSize (ig.Vec2 245 80) ig.Cond.Always
    ig.Begin "fps" null
        enum-bitfield ig.WindowFlags i32
            'NoDecoration
            'NoBackground
    ig.Text "FPS: %d" (bottle.time.get-fps)
    ig.Text "Time Scale: %.2f" (bottle.time.get-global-time-scale)
    ig.End;
    ()

@@ 'on bottle.log-write
fn (lineinfo prefix args...)
    print lineinfo prefix args...

do
    local-scope;
