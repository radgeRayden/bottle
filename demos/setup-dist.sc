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
            "scopesrt"
            "fontdue_native"
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

bin-dir := f"${module-dir}/dist/bin"
obj-dir := f"${module-dir}/dist/obj"

system f"mkdir -p ${bin-dir}"
system f"mkdir -p ${obj-dir}"

va-map
    inline copy-lib (path)
        system f"cp ${path} ${bin-dir}/"
    libpaths...

libpaths... :=
    va-map
        inline get-libpath (libname)
            find-library libname (list (bin-dir as string))
        libs...

libflags :=
    static-fold (libs = S"") for lib in (va-each libpaths...)
        f"${libs} -l:$(basename ${lib}) "

fn setup-dist ()
sugar-if main-module?
    setup-dist;
else
    do
        let libpaths...
        let bin-dir obj-dir libflags
        local-scope;
