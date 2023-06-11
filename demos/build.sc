using import String
using import C.stdlib
using import radl.strfmt

bottle := __env.bottle
using import .setup-dist

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
        f"${obj-dir}/${obj-name}" as string
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
                gcc -o ${bin-dir}/${exe-name} ${obj-dir}/${obj-name} -lm -L${bin-dir} ${libflags} -Wl,-rpath '-Wl,$ORIGIN'
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
