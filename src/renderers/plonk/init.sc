using import Option
using import struct

using import ...gpu.types
using import .SpriteBatch

struct PlonkState
    batch : SpriteBatch

global context : (Option PlonkState)

fn init ()
    context = (PlonkState)

fn sprite (atlas x y color)
    ctx := 'force-unwrap context
    'add-sprite ctx.batch atlas.texture-view x y ('get-quad atlas) color

fn submit (render-pass)

do
    let init sprite submit
    local-scope;
