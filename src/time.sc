sdl := import sdl3
using import .context
cfg := context-accessor 'config 'time

global first-time-measure : f64
global last-time-measure : f64
global delta-time : f64
global fixed-timestep : f64

fn get-raw-time ()
    now := ((sdl.GetPerformanceCounter) as f64) / (sdl.GetPerformanceFrequency) as f64
    now - first-time-measure

fn init ()
    first-time-measure = (get-raw-time)
    last-time-measure = (get-raw-time)
    fixed-timestep = cfg.fixed-timestep

# time scaling variables
# time-scale is a modifier that alters the result of delta-time and get-time
# use "raw" variants of the functions to get overall program runtime
global time-scale : f64 = 1.0
# time-offset is the raw program time when scale was changed, used to take
# the difference and scale time from then to now
global time-offset : f64
# scaled-time-offset is the scaled time at the time when scale was changed.
# This value is added to scaled time to eliminate a discontinuity.
global scaled-time-offset : f64

fn get-time ()
    scaled-time-offset + ((get-raw-time) - time-offset) * time-scale

fn set-global-time-scale (scale)
    # this has to come first because it changes a variable used by get-time
    scaled-time-offset = (get-time)
    time-offset = (get-raw-time)
    time-scale = scale

fn get-global-time-scale ()
    deref time-scale

fn get-delta-time ()
    delta-time * time-scale

fn get-fixed-timestep ()
    fixed-timestep * time-scale

fn get-raw-delta-time ()
    (deref delta-time)

fn get-fps ()
    (1 / (get-raw-delta-time)) as i32

fn step ()
    let now = (get-raw-time)
    delta-time = (now - last-time-measure)
    last-time-measure = now

fn sleep (milliseconds)
    sdl.Delay (milliseconds as u32)

do
    let init set-global-time-scale get-global-time-scale \
        get-time get-delta-time get-fixed-timestep get-raw-delta-time \
        get-fps step sleep
    local-scope;
