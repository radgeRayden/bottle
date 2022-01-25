import .src.runtime

let window = (import .src.window)
let gpu = (import .src.gpu)
let main = (import .src.main)
let sysevents = (import .src.sysevents)
let syscallbacks = (import .src.sysevents.callbacks)

vvv bind bottle
do
    from main let run load update draw
    using syscallbacks

    let
        window
        gpu
        sysevents

    locals;

bottle
