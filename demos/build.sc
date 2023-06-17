using import String
using import C.stdlib
using import radl.strfmt

bottle := __env.bottle
obj-dir := "./dist/obj"
bin-dir := "./dist/bin"

fn build-demo (name)
    module :=
        require-from module-dir name
            'bind __env 'bottle bottle

    local name : String = name as String
    for c in name
        if (c == char".")
            c = char"_"

    name as:= string

    main := (typify (module as Closure) i32 (@ rawstring))
    obj-name := .. "obj" name ".o"
    compile-object
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
    libflags := string (getenv "LDFLAGS")
    assert (libflags != null)

    cmd := f"gcc -o ${bin-dir}/${exe-name} ${obj-dir}/${obj-name} -I../include ../src/scopesrt.c -lm -L${bin-dir} ${libflags} -Wl,-rpath '-Wl,$ORIGIN'"
    print "+" cmd
    system cmd

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
