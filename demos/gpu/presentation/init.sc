using import glm struct
import bottle #...demo-common
ig := bottle.imgui

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "present modes"
    cfg.window.fullscreen? = true
    cfg.gpu.present-mode = 'FifoRelaxed

struct DemoState
    demo-step : i32
    test-started? : bool
    total-tests : i32

    current-test : i32
    current-sample : i32
    delay : f64
    last-time : f64
    reacted? : bool

    results : (array (array f64 10) 3)
    last-reaction-time : f64

global ctx : DemoState
global rng : bottle.random.RNG 0

@@ 'on bottle.load
fn ()
    bottle.time.sleep 1
    rng = typeinit (((bottle.time.get-time) * 10000) as u64)
    ctx.total-tests = 3

@@ 'on bottle.render
fn ()
    plonk := bottle.plonk
    WF := ig.WindowFlags
    ww wh := (bottle.window.get-size)

    inline draw-rectangle ()
        t := (bottle.time.get-time)
        if (t - ctx.last-time > ctx.delay)
            window-size := (vec2 ww wh)
            wcenter := window-size / 2
            rect-size := window-size / 5
            plonk.rectangle wcenter rect-size 0:f32
                ctx.reacted? (vec4 1 0 0 1) (vec4 1)

    inline instruction-slide (text)
        ig.SetNextWindowPos (ig.Vec2 (ww / 2) (wh / 2)) ig.Cond.Always (ig.Vec2 0.5 0.5)
        ig.SetNextWindowSize (ig.Vec2 600 400) ig.Cond.Always
        ig.Begin "slide" null
            WF.NoDecoration | WF.NoBackground
        ig.TextWrapped text
        if (ig.Button "Continue" (ig.Vec2 80 20))
            ctx.demo-step += 1
            ctx.delay = 2.0 + ('normalized rng)
            ctx.last-time = (bottle.time.get-time)
            ctx.test-started? = true
            ctx.reacted? = false
        ig.End;

    switch ctx.demo-step
    case 0
        if ((bottle.gpu.get-present-mode) != 'Fifo)
            bottle.gpu.set-present-mode 'Fifo
        instruction-slide
            """"This is a latency test. After you press continue, a white rectangle will flash on screen on irregular intervals.
                Press the spacebar as soon as you can after the rectangle appears. After 10 samples, the presentation mode will be
                changed and the test will be conducted again. At the end the averaged results will be presented. Note that only the difference
                between the results is relevant, to account for human reaction times.
    case 1
        draw-rectangle;
    case 2
        if ((bottle.gpu.get-present-mode) != 'FifoRelaxed)
            bottle.gpu.set-present-mode 'FifoRelaxed
        instruction-slide "Click Continue to begin the next test. The instructions are the same."
    case 3
        draw-rectangle;
    case 4
        if ((bottle.gpu.get-present-mode) != 'Immediate)
            bottle.gpu.set-present-mode 'Immediate
        instruction-slide "Click Continue to begin the next test. The instructions are the same."
    case 5
        draw-rectangle;
    case 6
        if ((bottle.gpu.get-present-mode) != 'Fifo)
            bottle.gpu.set-present-mode 'Fifo
        ig.SetNextWindowPos (ig.Vec2 (ww / 2) (wh / 2)) ig.Cond.Always (ig.Vec2 0.5 0.5)
        ig.SetNextWindowSize (ig.Vec2 600 400) ig.Cond.Always
        ig.Begin "slide" null
            WF.NoDecoration | WF.NoBackground

        inline calc-sample-average (idx)
            total :=
                fold (total = 0:f64) for sample in (ctx.results @ idx)
                    total + sample
            f32 (total / 10)

        ig.TextWrapped
            """"Test finished.
                Average latency for Fifo: %.3f
                Average latency for FifoRelaxed: %.3f
                Average latency for Immediate: %.3f
            calc-sample-average 0
            calc-sample-average 1
            calc-sample-average 2
        ig.End;
    default ()

@@ 'on bottle.key-pressed
fn (key)
    if (key == 'Space and ctx.test-started? and ctx.current-sample < 10 and (not ctx.reacted?))
        assert (ctx.current-test < (countof ctx.results))
        assert (ctx.current-sample < 10)

        t := (bottle.time.get-time)
        delta := t - ctx.last-time

        if (delta > ctx.delay)
            ctx.reacted? = true
            ctx.results @ ctx.current-test @ ctx.current-sample = delta - ctx.delay
            ctx.last-reaction-time = t

@@ 'on bottle.update
fn (dt)
    t := (bottle.time.get-time)
    if ctx.reacted?
        if (t - ctx.last-reaction-time > 1.0:f64)
            ctx.current-sample += 1
            ctx.reacted? = false
            ctx.last-time = t

    if (ctx.current-sample == 10)
        ctx.current-sample = 0
        ctx.current-test += 1
        ctx.demo-step += 1


sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
