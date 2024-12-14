switch operating-system
case 'linux
    shared-library "libSDL3_ttf.so"
case 'windows
    shared-library "SDL3_ttf.dll"
default
    error "Unsupported OS"

using import include

header := include "SDL3_ttf/SDL3_ttf.h"

vvv bind sdl3_ttf
do
    using header.define filter "^TTF_(.+)$"
    using header.typedef filter "^TTF_(.+)$"
    using header.define filter "^(SDL_TTF_.+)$"
    local-scope;

inline augment-enum (T prefix)
    local old-symbols : (Array Symbol)
    for k v in ('symbols T)
        field-name := k as Symbol as string
        match? start end := 'match? (.. str"^" prefix) field-name
        if match?
            new-name := rslice field-name end
            'set-symbol T (Symbol new-name) v
            'append old-symbols (k as Symbol)

    for s in old-symbols
        sc_type_del_symbol T s

augment-enum sdl3_ttf.HorizontalAlignment "TTF_HORIZONTAL_ALIGN_"
augment-enum sdl3_ttf.Direction "TTF_DIRECTION_"

sdl3_ttf
