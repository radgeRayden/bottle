using import Option
using import struct
ma := import miniaudio

global engine = undef ma.engine

fn init ()
    result := ma.engine_init null &engine
    assert (result == ma.MA_SUCCESS)

fn play-one-shot (name)
    ma.engine_play_sound &engine name null

fn shutdown ()
    ma.engine_uninit &engine

do
    let init shutdown play-one-shot
    local-scope;
