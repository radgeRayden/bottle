using import Buffer String
stbi := import stb.image

import ..filesystem
using import ..filesystem.FileStream
using import .ImageData
using import ..gpu.types

fn... load-image (filename : String)
    local w : i32
    local h : i32
    local channels : i32

    file := FileStream filename FileMode.Read
    filedata := 'read-all-bytes file
    ptr count := 'data filedata
    data := stbi.load_from_memory ptr (count as i32) &w &h &channels 4

    # TODO: raise error
    assert (data != null)

    wrapped-data := (Buffer (mutable@ u8)) data (w * h * 4)
        fn (ptr)
            stbi.image_free (ptr as voidstar)
    ImageData (w as u32) (h as u32) (slices = 1:u32) (data = wrapped-data)
case (filedata : (Buffer (mutable@ u8)))
    local w : i32
    local h : i32
    local channels : i32

    ptr count := 'data filedata
    data := stbi.load_from_memory ptr (count as i32) &w &h &channels 4

    # TODO: raise error
    assert (data != null)

    wrapped-data := (Buffer (mutable@ u8)) data (w * h * 4)
        fn (ptr)
            stbi.image_free (ptr as voidstar)
    ImageData (w as u32) (h as u32) (slices = 1:u32) (data = wrapped-data)

do
    let load-image ImageData
    local-scope;
