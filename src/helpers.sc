inline... &local (T : type, ...)
    &
        local T
            ...
case (value)
    &
        local dummy-name = value

inline array->ptr (value)
    using import Array

    static-match (superof value)
    case Array
        _ (countof value) (imply value pointer)
    case array
        local v = value
        _ (countof v) &v
    default
        local v = value
        _ 1 &v


@@ memo
inline param? (pT)
    typedef (.. "param?<" (tostring pT) ">")
        inline __typematch (cls T)
            dump cls T
            static-if ((imply? T pT) or (T == Nothing))
                true
            else false
        inline __rimply (cls T)
            inline (self) self

local-scope;
