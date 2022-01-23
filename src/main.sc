let sdl = (import .FFI.sdl)

import .window
import .gpu

fn run ()
    window.init;
    gpu.init;

    :: main-loop
    loop ()
        local event : sdl.Event
        while (sdl.PollEvent &event)
            if (event.type == sdl.SDL_QUIT)
                merge main-loop
            gpu.present;
    main-loop ::
    ;

do
    let run
    locals;
