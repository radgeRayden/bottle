import sdl
using import .config
cfg := cfg-accessor 'time

global first-time-measure : f64
global last-time-measure : f64
global delta-time : f64
global fixed-timestep : f64

fn get-time-raw ()
    ((sdl.GetPerformanceCounter) as f64) / (sdl.GetPerformanceFrequency) as f64

fn get-time ()
    (get-time-raw) - first-time-measure

fn init ()
    first-time-measure = (get-time-raw)
    last-time-measure = (get-time)
    fixed-timestep = cfg.fixed-timestep

fn get-delta-time ()
    deref delta-time

fn get-fixed-timestep ()
    deref fixed-timestep

fn step ()
    let now = (get-time)
    delta-time = (now - last-time-measure)
    last-time-measure = now

do
    let init get-delta-time step get-time
    locals;
