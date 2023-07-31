'bind-symbols __env
    module-search-path =
        ..
            list
                module-dir .. "/src/?/init.sc"
                module-dir .. "/src/?.sc"
            __env.module-search-path
    use-hardcoded-root? = true
