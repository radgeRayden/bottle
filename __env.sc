bottle := require-from module-dir ".init" __env

'bind-symbols __env
    bottle = bottle
    module-search-path =
        ..
            list
                module-dir .. "/src/?/init.sc"
                module-dir .. "/src/?.sc"
            __env.module-search-path
