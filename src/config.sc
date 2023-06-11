using import struct
using import Option
using import String

using import .enums

struct BottleConfig
    window :
        struct WindowConfig
            title = S"Game from Scratch Re:Birth"
            width  : (Option i32)
            height : (Option i32)
            relative-width  = 0.5
            relative-height = 0.5

            # initialization options
            fullscreen? = false
            hidden? = false
            borderless? = false
            resizable? = true
            minimized? = false
            maximized? = false
            always-on-top? = false
    time :
        struct TimeConfig
            use-delta-accumulator? : bool
            fixed-timestep : f64 = (1 / 60)
    gpu :
        struct GPUConfig
            power-preference = PowerPreference.HighPerformance

global istate-cfg : BottleConfig

do
    let istate-cfg
    locals;
