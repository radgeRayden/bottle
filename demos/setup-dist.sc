using import String
using import C.stdlib
using import radl.strfmt

libs... :=
    va-map
        inline (libname)
            let filename =
                static-match operating-system
                case 'windows
                    f"${libname}.dll"
                case 'linux
                    f"lib${libname}.so"
                default
                    error "unsupported OS"
            filename as string
        _
            "physfs"
            # "scopesrt"
            "fontdue_native"
            "miniaudio"
            "SDL2"
            "stb"
            "wgpu_native"
            "cimgui"

patched-search-path := (cons (.. compiler-dir "/bin") __env.library-search-path)
libpaths... :=
    va-map
        inline get-libpath (libname)
            find-library libname patched-search-path
        libs...

va-map
    inline copy-lib (path)
        system f"cp \"${path}\" ./dist/bin"
    libpaths...

libflags :=
    static-fold (libs = S"") for lib in (va-each libs...)
        f"${libs} -l:${lib} "

sc_write libflags
