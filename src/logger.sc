using import .callbacks enum FunctionChain print slice String radl.strfmt
String+ := import radl.String+

enum LogLevel plain
    Debug
    Info
    Warning
    Fatal

min-level := LogLevel.Debug

vvv bind prefixes
do
    Debug := sc_default_styler 'style-comment "DEBUG:"
    Info := sc_default_styler 'style-none "INFO:"
    Warning := sc_default_styler 'style-warning "WARNING:"
    Fatal := sc_default_styler 'style-error "FATAL ERROR:"
    local-scope;

run-stage;

inline make-log-macro (level anchor?)
    spice (...)
        argc := 'argcount args
        if (min-level <= (getattr LogLevel level) or argc == 0)
            let args anchor =
                static-if anchor?
                    _
                        sc_argument_list_map_new (argc - 1)
                            inline (i)
                                let i = (i + 1)
                                'getarg args i
                        ('getarg args 0) as Anchor
                else
                    _ args ('anchor args)

            prefix := '@ prefixes level
            path := (sc_anchor_path anchor) as string
            relpath := rslice path (countof (String+.common-prefix (String path) (String module-dir)))
            lineinfo := f"${relpath}:${sc_anchor_lineno anchor}:${sc_anchor_column anchor}:" as string
            `(log-write [lineinfo] [prefix] args)
        else
            `()

levels... := va-map ((x) -> x.Name) LogLevel.__fields__
let log-functions =
    static-fold (logger-functions = (Scope)) for level in (va-each levels...)
        name := .. "write-" ((String+.ASCII-tolower (level as string)) as string)
        logger-functions := 'bind logger-functions (Symbol name) (make-log-macro level false)

        name := name .. "@"
        'bind logger-functions (Symbol name) (make-log-macro level true)
run-stage;

do
    using log-functions
    local-scope;
