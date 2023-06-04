'bind-symbols __env
    bottle = import .init
    module-search-path =
        ..
            list
                module-dir .. "/src/?/init.sc"
                module-dir .. "/src/?.sc"
            __env.module-search-path
