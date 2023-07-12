using import Array
stbi := import stb.image

import ..filesystem
using import .ImageData

fn load-image (filename)
    local w : i32
    local h : i32
    local channels : i32

    filedata := filesystem.load-file filename
    data := stbi.load_from_memory filedata ((countof filedata) as i32) &w &h &channels 4

    # TODO: raise error
    assert (data != null)

    wrapped-data := 'wrap (Array u8) data (w * h * 4)
    free data

    ImageData (w as u32) (h as u32) (slices = 1:u32) (data = wrapped-data)

do
    let load-image ImageData
    local-scope;
