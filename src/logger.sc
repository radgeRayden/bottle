import C.stdio
using import String

typedef+ bool
    inline __tostring (self)
        ? self &"true" &"false"

fn print (...)
    C.stdio.printf
        ..
            va-lfold ""
                inline (k next result)
                    .. result "%s" " "
                ...
            "\n"
        va-map
            inline (arg)
                T := (typeof arg)
                static-match T
                case String
                    arg as rawstring
                case rawstring
                    arg
                default
                    static-if (T < zarray)
                        arg as rawstring
                    else
                        (tostring arg) as rawstring
            ...

spice report (args...)
    anchor := (tostring `[('anchor args)])
    spice-quote
        print [anchor] args...
        view args...

do
    let print report
    locals;
