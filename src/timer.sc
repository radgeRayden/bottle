import sdl
import .config

global last-time-measure : f64
global delta-time : f64
global fixed-timestep : f64

fn get-time ()
    ((sdl.GetPerformanceCounter) as f64) / ((sdl.GetPerformanceFrequency) as f64)

fn init ()
    cfg := config.istate-cfg

    last-time-measure = (get-time)
    fixed-timestep = cfg.timer.fixed-timestep

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
