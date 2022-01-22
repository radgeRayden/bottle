inline lib (name)
    try
        load-library (.. module-dir "/../runtime/" name)
    else
        try
            load-library name
        except (ex)
            'dump ex
            hide-traceback;
            error 
                .. "There was a problem loading a shared library : "
                    name
                    ". Did you build or install the binary dependencies for bottle?"

switch operating-system
case 'linux
    # lib "libbottle.so"
    lib "libSDL2.so"
    lib "libwgpu_native.so"
case 'windows
    # lib "libbottle.dll"
    lib "SDL2.dll"
    lib "wgpu_native.dll"
default
    error "Unsupported OS."
