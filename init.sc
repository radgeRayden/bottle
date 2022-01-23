import .src.runtime

let window = (import .src.window)
let gpu = (import .src.gpu)
let main = (import .src.main)
let sysevents = (import .src.sysevents)

vvv bind bottle
do
    let run = main.run
    let
        window
        gpu
        sysevents

    locals;

bottle
