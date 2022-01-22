import .filter-scope

let header =
    include
        """"#include <SDL2/SDL.h>
            #include <SDL2/SDL_syswm.h>
        options
            "-I" .. module-dir .. "../../dependencies/SDL/"

let sdl-extern = (filter-scope header.extern "^SDL_")
let sdl-typedef = (filter-scope header.typedef "^SDL_")
let sdl-define = (filter-scope header.define "^(?=SDL_)")
let sdl-const = (filter-scope header.const "^(?=SDLK?_)")

let sdl = (.. sdl-extern sdl-typedef sdl-define sdl-const)
run-stage;

let sdl-macros =
    do
        inline SDL_WINDOWPOS_UNDEFINED_DISPLAY (X)
            sdl.SDL_WINDOWPOS_UNDEFINED_MASK | X

        inline SDL_WINDOWPOS_ISUNDEFINED (X)
            (X & 0xFFFF0000) == sdl.SDL_WINDOWPOS_UNDEFINED_MASK

        inline SDL_WINDOWPOS_CENTERED_DISPLAY (X)
            sdl.SDL_WINDOWPOS_CENTERED_MASK | X

        inline SDL_WINDOWPOS_ISCENTERED (X)
            (X & 0xFFFF0000) == sdl.SDL_WINDOWPOS_CENTERED_MASK

        let SDL_WINDOWPOS_CENTERED = (SDL_WINDOWPOS_CENTERED_DISPLAY 0)
        let SDL_WINDOWPOS_UNDEFINED = (SDL_WINDOWPOS_UNDEFINED_DISPLAY 0)

        inline SDL_VERSION (version-struct)
            version-struct.major = sdl.SDL_MAJOR_VERSION
            version-struct.minor = sdl.SDL_MINOR_VERSION
            version-struct.patch = sdl.SDL_PATCHLEVEL
            ;

        inline SDL_VERSIONNUM (major minor patch)
            (major * 1000) + (minor * 100) + patch

        inline SDL_COMPILEDVERSION ()
            SDL_VERSIONNUM sdl.SDL_MAJOR_VERSION sdl.SDL_MINOR_VERSION sdl.SDL_PATCHLEVEL

        inline SDL_VERSION_ATLEAST (major minor patch)
            (SDL_COMPILEDVERSION) >= (SDL_VERSIONNUM major minor patch)
        locals;

inline enum-constructor (T)
    bitcast 0 T

for scope in ('lineage sdl)
    for k v in scope
        if (('typeof v) == type)
            v as:= type
            if (v < CEnum)
                'set-symbol v '__typecall enum-constructor

# Type augmentations so that SDL types work seamlessly across the codebase.
# =========================================================================
do
    typedef+ sdl.bool
        inline __imply (lhsT rhsT)
            static-if (rhsT == bool)
                inline (self)
                    (bitcast self i32) as bool

    # necessary so these work as Map keys.
    typedef+ sdl.WindowEventID
        inline __rimply (lhs rhs)
            static-if (lhs == u8)
                inline (other self)
                    bitcast (other as (storageof this-type)) this-type
            else
                super-type.__ras lhs rhs

        inline __== (lhs rhs)
            static-if (rhs == u8)
                inline (self other)
                    (storagecast self) == other
            else
                super-type.__== lhs rhs

sdl .. sdl-macros
