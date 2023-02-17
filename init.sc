let window = (import .src.window)
let gpu = (import .src.gpu)
let timer = (import .src.timer)
let main = (import .src.main)
let sysevents = (import .src.sysevents)
let syscallbacks = (import .src.sysevents.callbacks)
let enums = (import .src.enums)

vvv bind bottle
do
    using main
    using syscallbacks

    let
        window
        gpu
        enums
        timer

    let quit! = sysevents.quit

    locals;

bottle
