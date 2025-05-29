using import FunctionChain struct .context

ctx := context-accessor 'callbacks

spice callback-name (name)
    `[(name as Symbol as string)]

spice chain-callback (T f)
    T as:= type
    try ('@ T 'callback)
    then (parent)
        let newf =
            spice-quote
                fn (...)
                    parent ...
                    f ...
        'set-symbol T 'callback newf
        newf
    else
        'set-symbol T 'callback f
        f

run-stage;

typedef BottleCallback
    inline __typecall (cls name f)
        typedef (.. "BottleCallback" ":" (tostring name))
            default-callback := f

            inline on (self)
                inline (f)
                    chain-callback self f

            inline __typecall (cls ...)
                (getattr ctx name) ...

            inline replace (self f)
                (getattr ctx name) = f

typedef BottleCallbackDefinitions

spice add-callback (name)
    name as:= Symbol
    f := fn (...) ()
    sc_template_set_name (sc_closure_get_template f) name
    spice-quote
        'set-symbol BottleCallbackDefinitions [name] (BottleCallback [name] [f])

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
    quit := (BottleCallback 'quit (fn "quit" (...) true))

spice assign-callbacks ()
    expr := (sc_expression_new)
    for k v in ('symbols BottleCallbackDefinitions)
        k as:= Symbol
        v as:= type
        let f =
            try ('@ v 'callback)
            else ('@ v 'default-callback)
        sc_expression_append expr
            spice-quote
                (getattr ctx [k]) = [f]
    expr

run-stage;

do
    using (mixin BottleCallbackDefinitions)
    let assign-callbacks

    local-scope;
