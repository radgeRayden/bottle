let audio = (import .src.audio)
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
let math = (import .src.math)
let mouse = (import .src.mouse)
let plonk = (import .src.plonk)
let random = (import radl.random)
let sysevents = (import .src.sysevents)
let time = (import .src.time)
let types = (import .src.types)
let window = (import .src.window)

VERSION :=
    label get-version
        try
            using import radl.IO
            version-file := FileStream (module-dir .. "/BOTTLE_VERSION") FileMode.Read
            merge get-version ('read-all-string version-file)
        else ()

        using import radl.gitversion
        git-version;

VERSION as:= string
run-stage;

fn get-version ()
    VERSION

vvv bind bottle
..
    main
    callbacks
    do
        let
            audio
            asset
            enums
            exceptions
            filesystem
            font
            gpu
            input
            keyboard
            logger
            math
            mouse
            plonk
            random
            time
            types
            window

        let quit! = sysevents.quit
        let get-version

        locals;

bottle
