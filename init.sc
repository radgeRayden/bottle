let enums = (import .src.enums)
let font = (import .src.font)
let gpu = (import .src.gpu)
let input = (import .src.input)
let keyboard = (import .src.keyboard)
let main = (import .src.main)
let mouse = (import .src.mouse)
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
        input
        keyboard
        mouse
        timer
        window

    let quit! = sysevents.quit

    locals;

bottle
