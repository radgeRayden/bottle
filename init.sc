let window = (import .src.window)
let gpu = (import .src.gpu)
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

    let quit! = sysevents.quit

    locals;

bottle
