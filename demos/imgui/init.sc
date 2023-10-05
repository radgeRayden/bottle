using import struct
import ..demo-common
import bottle
ig := bottle.imgui

struct GuiState
    window-opened? : bool = true
global gui-state : GuiState

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "imgui demo"
    cfg.enabled-modules.plonk = false
    ;

@@ 'on bottle.render
fn ()
    if gui-state.window-opened?
        ig.Begin "file explorer" &gui-state.window-opened? 0
        ig.Text "hello world"
        ig.End;

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
