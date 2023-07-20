using import Option
using import struct
ma := import miniaudio
import .filesystem

global engine = undef ma.engine

fn init ()
    result := ma.engine_init null &engine
    assert (result == ma.MA_SUCCESS)

fn play-one-shot (name)
    try (ma.engine_play_sound &engine (filesystem.realpath name) null) ()
    else ()

fn shutdown ()
    ma.engine_uninit &engine

do
    let init shutdown play-one-shot
    local-scope;
