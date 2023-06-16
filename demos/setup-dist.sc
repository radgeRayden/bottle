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
            "scopesrt"
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

local module-dir : String = module-dir
for c in module-dir
    if (c == char"\\")
        c = char"/"

bin-dir := f"./dist/bin"
obj-dir := f"./dist/obj"

fn cmd (command)
    system (report command)

cmd f"mkdir \"${module-dir}/dist\""
cmd f"mkdir \"${bin-dir}\""
cmd f"mkdir \"${obj-dir}\""

va-map
    inline copy-lib (path)
        cmd f"cp \"${path}\" \"${bin-dir}\"/"
    libpaths...

libpaths... :=
    va-map
        inline get-libpath (libname)
            find-library libname (list (bin-dir as string))
        libs...

libflags :=
    static-fold (libs = S"") for lib in (va-each libs...)
        f"${libs} -l:${lib} "

fn setup-dist ()
sugar-if main-module?
    setup-dist;
else
    do
        let libpaths...
        let bin-dir obj-dir libflags cmd module-dir
        local-scope;
