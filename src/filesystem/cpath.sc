switch operating-system
case 'linux
    shared-library "libcpath.so"
case 'windows
    shared-library "cpath.dll"
default
    error "Unsupported OS"

header :=
    include "cpath.h"
        options "-D_CPATH_FUNC_=extern"

inline filter-scope (scope pattern)
    pattern as:= string
    fold (scope = (Scope)) for k v in scope
        let name = (k as Symbol as string)
        let match? start end = ('match? pattern name)
        if match?
            'bind scope (Symbol (rslice name end)) v
        else
            scope

cpath-extern := filter-scope header.extern "^cpath(?=[^_])"
cpath-typedef := filter-scope header.typedef "^(?=cpath)"

'bind-symbols (cpath-extern .. cpath-typedef)
    Traverse = header.extern.cpath_traverse
