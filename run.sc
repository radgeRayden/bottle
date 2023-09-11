using import String

config := import .src.config
cfg := config.config

name argc argv := (script-launch-args)
let demo =
    if (argc > 0)
        'from-rawstring String (argv @ 0)
    else
        S"gpu.hello-triangle"

use-genc? := (argc > 1) and (('from-rawstring String (argv @ 1)) == "-genc")

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
