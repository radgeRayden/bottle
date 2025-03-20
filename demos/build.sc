using import String C.stdlib radl.strfmt print

obj-dir := module-dir .. "/dist/obj"
bin-dir := module-dir .. "/dist/bin"

inline build-demo (name use-genc?)
    module :=
        require-from module-dir name 
            'bind-symbols __env
                use-hardcoded-root? = false

    local name : String = name as String
    for c in name
        if (c == char".")
            c = char"_"

    name as:= string

    inline typify-main (f)
        typify (f as Closure) i32 (@ rawstring)

    let main extra-lflags = 
        if (('typeof module) == Closure)
            _ (typify-main module) S""
        else
            scope := (module as Scope)
            main := typify-main ('@ scope 'main)
            try (('@ scope 'extra-lflags) as String)
            then (extra-lflags)
                _ main (copy extra-lflags)
            else
                _ main S""

    obj-name := .. "obj" name ".o"

    let compilef = compile-object
        # static-if use-genc?
        #     (import compiler.target.C) . compile-object
        # else
        #     compile-object

    compilef
        default-target-triple
        compiler-file-kind-object
        f"${obj-dir}/${obj-name}" as string
        do
            main := main
            local-scope;
        # 'dump-module

    let exe-name =
        static-match operating-system
        case 'windows
            f"demo${name}.exe"
        default
            f"demo${name}"

    inline cmd (cmd)
    libflags := (getenv "LDFLAGS")
    assert (libflags != null)
    libflags := string libflags

    cmd := f"gcc -o ${bin-dir}/${exe-name} ${obj-dir}/${obj-name} -I../include -lm ${extra-lflags} -L${bin-dir} ${libflags} -Wl,-rpath '-Wl,$ORIGIN'"
    print "+" cmd
    status := system cmd
    if (status == -1)
        error "failed to execute compilation command"
    else
        status >> 8

name argc argv := (script-launch-args)
let demo =
    if (argc > 0)
        string (argv @ 0)
    else
        error "missing demo argument"
use-genc? := (argc > 1) and (('from-rawstring String (argv @ 1)) == "-genc")
run-stage;

build-demo demo use-genc?
