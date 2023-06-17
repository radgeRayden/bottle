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
        _popen _pclose popen pclose := # hack for windows-mingw
        using import C.stdlib
        using import C.stdio
        using import String

        let popen pclose =
            static-if (operating-system == 'windows) (_ _popen _pclose)
            else (_ popen pclose)

        try
            using import radl.IO
            version-file := FileStream (module-dir .. "/BOTTLE_VERSION") FileMode.Read
            merge get-version ('read-all-string version-file)
        else ()

        inline try-commands (def cmd...)
            let devnull =
                static-if (operating-system == 'windows) str"NUL"
                else str"/dev/null"
            va-map
                inline "#hidden" (cmd)
                    handle := popen (.. "bash -c '" cmd "' 2> " devnull) "r"
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
            "git describe --exact-match --tags HEAD"
            "echo git-$(git rev-parse --short HEAD)-$(git rev-parse --abbrev-ref HEAD)"
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
