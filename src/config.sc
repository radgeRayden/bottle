using import struct
using import String

struct BottleConfig
    window :
        struct WindowConfig
            title = S"Game from Scratch Re:Birth"
            width = 640
            height = 480

global istate-cfg : BottleConfig

do
    let istate-cfg
    locals;
