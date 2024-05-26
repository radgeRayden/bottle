import C.stdio
using import print radl.ext radl.strfmt String
using import ..src.context

import bottle sdl3

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
fn demo-info ()
    ig := import ..src.imgui
    ig.SetNextWindowPos (ig.Vec2 10 10) ig.Cond.Always (ig.Vec2 0 0)
    ig.SetNextWindowSize (ig.Vec2 245 300) ig.Cond.Always
    ig.Begin "demo-common.fps" null
        enum-bitfield ig.WindowFlags i32
            'NoDecoration
            'NoBackground
    ig.Text "FPS: %d" (bottle.time.get-fps)
    ig.Text "Time Scale: %.2f" (bottle.time.get-global-time-scale)
    rep := (bottle.gpu.generate-report)

    inline n (r)
        r.numKeptFromUser

    ig.Text
        """"textures: %llu
            textureViews: %llu
            samplers: %llu
            bg-layouts: %llu
            pip-layouts: %llu
        n rep.textures
        n rep.textureViews
        n rep.samplers
        n rep.bindGroupLayouts
        n rep.pipelineLayouts
    ig.End;

    ww wh := (bottle.window.get-size)
    ig.SetNextWindowPos (ig.Vec2 ((f32 ww) - 120) 10) ig.Cond.Always (ig.Vec2 0 0)
    ig.Begin "demo-common.version" null
        enum-bitfield ig.WindowFlags i32
            'NoDecoration
            'NoBackground
    ig.Text "%s" ((bottle.get-version) as rawstring)
    ig.End;
    ()

@@ 'on bottle.log-write
fn (log-level lineinfo prefix args...)
    if (log-level == 'Fatal)
        window := context-accessor 'window 'handle
        sdl3.ShowSimpleMessageBox sdl3.SDL_MESSAGEBOX_ERROR "FATAL ERROR" f"${args...}" (window)
    else
        print lineinfo prefix args...

do
    local-scope;
