inline... &local (T : type, ...)
    &
        local T
            ...
case (value)
    &
        local dummy-name = value

@@ memo
inline param? (pT)
    typedef (.. "param?<" (tostring pT) ">")
        inline __typematch (cls T)
            static-if ((typematch? T pT) or (T == Nothing))
                true
            else false
        inline __rimply (cls T)
            inline (self)
                static-if (not (none? self)) (imply self pT)
                else none

spice static-hash (str)
    using import hash
    `[(hash (str as string))]

fn tolower (str)
    using import String

    delta := char"a" - char"A"
    local result : String
    for c in str
        if (c >= char"A" and c <= char"Z")
            'append result (c + delta)
        else
            'append result c
    result

spice static-tolower (str)
    `[((tolower (str as string)) as string)]

run-stage;

inline match-string-enum (enum-type value cases...)
    using import hash
    using import switcher
    using import print

    call
        switcher sw
            va-map
                inline (k)
                    case (static-eval (hash (tolower (k as string))))
                        getattr enum-type k
                cases...
            default
                raise;
        hash (tolower value)

do
    let &local
        param?
        match-string-enum
    local-scope;
