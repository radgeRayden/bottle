# it has to work even before everything has been installed.
let bottle =
    try ((require-from module-dir ".init" __env) as Scope)
    else (Scope)
run-stage;

'bind-symbols __env
    bottle = bottle
    module-search-path =
        ..
            list
                module-dir .. "/src/?/init.sc"
                module-dir .. "/src/?.sc"
            __env.module-search-path
