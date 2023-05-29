using import struct
using import Option
using import String

struct BottleConfig
    window :
        struct WindowConfig
            title = S"Game from Scratch Re:Birth"
            width  : (Option i32)
            height : (Option i32)
            relative-width  = 0.5
            relative-height = 0.5
    timer :
        struct TimerConfig
            use-delta-accumulator? : bool
            fixed-timestep : f64 = (1 / 60)

global istate-cfg : BottleConfig

do
    let istate-cfg
    locals;
