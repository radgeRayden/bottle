let asset = (import .src.asset)
let callbacks = (import .src.callbacks)
let enums = (import .src.enums)
let exceptions = (import .src.exceptions)
let filesystem = (import .src.filesystem)
let font = (import .src.font)
let gpu = (import .src.gpu)
let input = (import .src.input)
let keyboard = (import .src.keyboard)
let logger = (import .src.logger)
let main = (import .src.main)
let mouse = (import .src.mouse)
let renderers = (import .src.renderers)
let sysevents = (import .src.sysevents)
let time = (import .src.time)
let types = (import .src.types)
let window = (import .src.window)

vvv bind bottle
..
    main
    callbacks
    do
        let
            asset
            enums
            exceptions
            filesystem
            font
            gpu
            input
            keyboard
            logger
            mouse
            renderers
            time
            types
            window

        let quit! = sysevents.quit

        locals;

bottle
