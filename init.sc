let enums = (import .src.enums)
let font = (import .src.font)
let gpu = (import .src.gpu)
let main = (import .src.main)
let syscallbacks = (import .src.sysevents.callbacks)
let sysevents = (import .src.sysevents)
let timer = (import .src.timer)
let window = (import .src.window)

vvv bind bottle
do
    using main
    using syscallbacks

    let
        enums
        font
        gpu
        timer
        window

    let quit! = sysevents.quit

    locals;

bottle
