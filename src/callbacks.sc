using import struct .context

ctx := context-accessor 'callbacks

spice callback-name (name)
    `[(name as Symbol as string)]

spice chain-callback (T f)
    T as:= type
    try ('@ T 'CallbackInitExpression)
    then (expr)
        sc_expression_append expr `('append (getattr ctx T.Name) f)
        f
    else
        expr := (sc_expression_new)
        sc_expression_append expr `('append (getattr ctx T.Name) f)
        'set-symbol T 'CallbackInitExpression expr
        f

run-stage;

inline BottleCallback (name f)
    typedef (.. "BottleCallback" ":" (tostring name)) : (storageof Nothing)
        DefaultCallback := f
        Name := name

        inline on (self)
            inline (f)
                chain-callback this-type f

        inline __typecall (cls)
            bitcast none this-type

        inline __call (self ...)
            chain := getattr ctx Name
            fT := (typeof chain) . ElementType
            retT := returnof fT
            static-if (retT == void)
                for cb in chain (cb ...)
            else
                fold (result = (retT)) for cb in chain
                    cb ...

        inline replace (self f)
            chain := getattr ctx Name
            'clear chain
            'append chain f

        inline __= (lhs rhs)
            'replace lhs rhs

        inline chain (self f)
            'append (getattr ctx Name) f

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
        let init-expr =
            try 
                '@ T 'CallbackInitExpression
            else 
                f := '@ T 'DefaultCallback
                spice-quote
                    'append (getattr ctx [k]) [f]
        sc_expression_append expr init-expr
    expr

run-stage;

do
    using (mixin BottleCallbackDefinitions)
    let assign-callbacks

    local-scope;
