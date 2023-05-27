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

sugar copy-type (name super body...)
    qq
        [type] [name] < [super] :: ([storageof] [super])
            unquote-splice body...

local-scope;
