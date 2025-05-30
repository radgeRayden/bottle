using import FunctionChain struct .context

ctx := context-accessor 'callbacks

spice callback-name (name)
    `[(name as Symbol as string)]

spice chain-callback (T f)
    T as:= type
    try ('@ T 'Callback)
    then (parent)
        let newf =
            spice-quote
                fn (...)
                    parent ...
                    f ...
        'set-symbol T 'Callback newf
        newf
    else
        'set-symbol T 'Callback f
        f

run-stage;

inline BottleCallback (name f)
    typedef (.. "BottleCallback" ":" (tostring name)) : (storageof Nothing)
        DefaultCallback := f

        inline on (self)
            inline (f)
                chain-callback this-type f

        inline __typecall (cls)
            bitcast none this-type

        inline __call (cls ...)
            (getattr ctx name) ...

        inline replace (self f)
            (getattr ctx name) = f

        inline __= (lhs rhs)
            'replace lhs rhs

typedef BottleCallbackDefinitions

spice add-callback (name)
    name as:= Symbol
    f := fn (...) ()
    sc_template_set_name (sc_closure_get_template f) name
    spice-quote
        'set-symbol BottleCallbackDefinitions [name] ((BottleCallback [name] [f]))

run-stage;

let callbacks... =
    'configure
    'load
    'update
    'begin-frame
    'render
    'end-frame
    'log-write
    'controller-added
    'controller-axis-moved
    'controller-button-pressed
    'controller-button-released
    'controller-removed
    'key-pressed
    'key-released
    'mouse-moved
    'mouse-pressed
    'mouse-released
    'text-input
    'wheel-scrolled
    'window-resized

va-map add-callback callbacks...

# quit callback is special because it returns a value
type+ BottleCallbackDefinitions
    quit := ((BottleCallback 'quit (fn "quit" (...) true)))

spice assign-callbacks ()
    expr := (sc_expression_new)
    for k v in ('symbols BottleCallbackDefinitions)
        k as:= Symbol
        T := 'typeof v
        let f =
            try ('@ T 'Callback)
            else ('@ T 'DefaultCallback)
        sc_expression_append expr
            spice-quote
                (getattr ctx [k]) = [f]
    expr

run-stage;

do
    using (mixin BottleCallbackDefinitions)
    let assign-callbacks

    local-scope;
