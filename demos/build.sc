using import String
using import C.stdlib
using import radl.strfmt
bottle := __env.bottle

let libs... =
    "scopesrt"
    "fontdue_native"
    "SDL2"
    "stb"
    "wgpu_native"

patched-search-path := (cons (.. compiler-dir "/bin") __env.library-search-path)
inline copy-lib (libname)
    let filename =
        static-match operating-system
        case 'windows
            f"${libname}.dll"
        case 'linux
            f"lib${libname}.so"
        default
            error "unsupported OS"
    filename as:= string

    path := find-library filename patched-search-path
    system f"cp ${path} ${module-dir}/"

va-map copy-lib libs...

libs :=
    static-fold (libs = S"") for lib in (va-each libs...)
        .. libs "-l" lib " "
run-stage;

fn build-demo (name)
    module :=
        require-from module-dir name
            'bind __env 'bottle bottle

    main := (typify (module as Closure) i32 (@ rawstring))
    obj-name := .. "obj" name ".o"
    compile-object
        default-target-triple
        compiler-file-kind-object
        obj-name
        do
            main := main
            local-scope;

    let exe-name =
        static-match operating-system
        case 'windows
            f"demo${name}.exe"
        default
            f"demo${name}"

    system f""""pushd ${module-dir}
                gcc -o ${exe-name} ${obj-name} -L. ${libs} -Wl,-rpath '-Wl,$ORIGIN'
                popd

sugar-if main-module?
    name argc argv := (script-launch-args)
    if (argc > 0)
        build-demo (string (argv @ 0))
    else
        print "missing demo argument"
        -1
else
    do
        let build-demo
        local-scope;
