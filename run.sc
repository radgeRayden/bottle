using import String

name argc argv := (script-launch-args)
let demo =
    if (argc > 0)
        'from-rawstring String (argv @ 0)
    else
        S"gpu.hello-triangle"

local use-genc? : bool
local live-reload? : bool

if (argc > 1)
    for i in (range 1 argc)
        arg := 'from-rawstring String (argv @ i)
        match arg
        case "--genc"
            use-genc? = true
        case "--live-reload"
            live-reload? = true
        default
            ()

run-stage;

import-string := .. ".demos." demo

let module =
    try
        require-from module-dir import-string __env
    except(ex)
        'dump ex
        error (.. "failed to load demo: " (demo as string))

run-stage;

sugar-if use-genc?
    using import compiler.target.C
    hook-compile-function;

f := (compile (typify (module as Closure) i32 (@ rawstring))) as (@ (function i32 i32 (@ rawstring)))
f argc argv
0
