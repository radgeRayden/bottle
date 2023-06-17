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
        using import C.stdlib
        using import C.stdio
        using import String

        version := getenv "BOTTLE_VERSION"
        if (version != null)
            merge get-version (String version)

        inline try-commands (def cmd...)
            va-map
                inline "#hidden" (cmd)
                    handle := popen (cmd .. " 2> /dev/null") "r"
                    local result : String
                    while (not ((feof handle) as bool))
                        local c : i8
                        fread &c 1 1 handle
                        if (c != 0 and c != char"\n")
                            'append result c

                    if ((pclose handle) == 0)
                        return (deref result)
                    S""
                cmd...
            def

        try-commands S"unknown"
            "git describe --exact-match --tags"
            "echo git-$(git rev-parse --short HEAD)"
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
