import ..demo-common
import bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "font rendering"

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
