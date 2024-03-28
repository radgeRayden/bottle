import bottle
import ...demo-common
using import print radl.strfmt

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "mipmap generation benchmark"
    cfg.window.hidden? = true
    cfg.enabled-modules.imgui = false
    cfg.enabled-modules.plonk = false

global bench-time : f64
global mip-count : u32

@@ 'on bottle.load
fn ()
    using bottle.types
    try
        2dtex := Texture 4096 4096 (format = 'RGBA8Unorm) (mipmap-levels = 0)
        generations := 1000:u32
        mip-count = 2dtex.MipLevelCount * generations

        start := (bottle.time.get-raw-time)
        for i in (range generations)
            'generate-mipmaps 2dtex
        end := (bottle.time.get-raw-time)
        bench-time = end - start
    except (ex)
        ()
    bottle.quit!;

@@ 'on bottle.quit
fn ()
    bottle.logger.write-info f"time taken to generate ${mip-count} mipmaps: ${bench-time}"
    true

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
