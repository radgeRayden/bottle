using import .build-config print slice String radl.strfmt
String+ := import radl.String+

min-level := BottleBuildConfig.MinimumLogLevel

vvv bind prefixes
do
    Debug := sc_default_styler 'style-comment "DEBUG:"
    Info := sc_default_styler 'style-none "INFO:"
    Warning := sc_default_styler 'style-warning "WARNING:"
    Fatal := sc_default_styler 'style-error "FATAL ERROR:"
    local-scope;

run-stage;

inline make-log-macro (level)
    spice (message)
        if (min-level <= (getattr LogLevel level))
            anchor := 'anchor args
            prefix := '@ prefixes level
            path := (sc_anchor_path anchor) as string
            relpath := rslice path (countof (String+.common-prefix (String path) (String module-dir)))
            lineinfo := f"${relpath}:${sc_anchor_lineno anchor}:${sc_anchor_column anchor}:" as string
            `(print2 [lineinfo] [prefix] message)
        else
            `()

levels... := va-map ((x) -> x.Name) LogLevel.__fields__
static-fold (logger-functions = (Scope)) for level in (va-each levels...)
    name := .. "write-" ((String+.ASCII-tolower (level as string)) as string)
    'bind logger-functions (Symbol name) (make-log-macro level)
