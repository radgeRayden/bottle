import .src.runtime

let window = (import .src.window)
let gpu = (import .src.gpu)
let main = (import .src.main)
let sysevents = (import .src.sysevents)
let callbacks = (import .src.sysevents.callbacks)

vvv bind bottle
do
    let run = main.run
    using callbacks

    let
        window
        gpu
        sysevents

    locals;

bottle
