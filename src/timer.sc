import sdl
global last-time-measure : f64
global delta-time : f64

fn get-time ()
    ((sdl.GetPerformanceCounter) as f64) / ((sdl.GetPerformanceFrequency) as f64)

fn init ()
    last-time-measure = (get-time)


fn get-delta-time ()
    deref delta-time

fn step ()
    let now = (get-time)
    delta-time = (now - last-time-measure)
    last-time-measure = now

do
    let init get-delta-time step get-time
    locals;
