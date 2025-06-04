using import Array radl.FileWatcher struct .context print radl.strfmt radl.ext radl.shorthands
import .main .callbacks .time .exceptions

ctx := context-accessor 'live
callbacks-ctx := context-accessor 'callbacks

struct LiveGlobalVariable
    data : voidstar
    dropf : (@ (function void voidstar))

    inline __drop (self)
        self.dropf self.data

global module-storage : (Array LiveGlobalVariable)

@@ memo
inline gen-dropf (T)
    fn (ptr)
        self := ptrtoref (ptr as (@ T))
        drop self

inline new-live-variable (var)
    'append module-storage
        LiveGlobalVariable
            data = (dupe &var) as voidstar
            dropf = gen-dropf (typeof var)
    var

sugar live-variable (name rest...)
    sym := 'unique Symbol "global"
    qq 
        let [name] =
            do
                let [sym] = [global]
                [sym] [name] (unquote-splice rest...)
                [new-live-variable] [name]

run-stage;

let module-scope =
    do
        let global = live-variable
        local-scope;

fn reload-module ()
    (callbacks-ctx) = copy ctx.default-callbacks
    load-module (ctx.name as string) (ctx.path as string) __env
    callbacks.load;

fn strip-color-codes (str)
    using import radl.String+ slice
    loop (result = str)
        has-code? start end := scan result "\x1b"
        if has-code?
            lhs rhs := lslice result start, rslice result end
            code-complete? start end := scan rhs "m"
            if code-complete?
                .. lhs (rslice rhs end)
            else (return result)
        else
            return result

fn on-file-update (path ev-type)
    'clear module-storage
    if (ev-type == 'Modified)
        try
            reload-module;
            ctx.last-error = ""
            ()
        except (ex)
            err := 'format ex
            print err
            ctx.last-error = (strip-color-codes err)

@@ 'on callbacks.update
fn process-file-events (dt)
    if ctx.first-load?
        on-file-update ctx.path FileEventType.Modified
        ctx.first-load? = false
    else
        any-events? := 'dispatch-events ctx.watcher
        if any-events? (print f"source reloaded: ${ctx.name}. time: ${(time.get-raw-time)}")
    ()

@@ 'on callbacks.render
fn ()
    raising exceptions.GPUError
    ig := import .imgui
    import .window

    ww wh := (window.get-size)
    ig.SetNextWindowPos (ig.Vec2 0 0) ig.Cond.Always (ig.Vec2 0 0)
    ig.SetNextWindowSize (ig.Vec2 (|> f32 ww wh)) ig.Cond.Always
    ig.Begin "live.error-display" null
        enum-bitfield ig.WindowFlags i32
            'NoDecoration
            'NoBackground
    ig.Text "%s" ((view ctx.last-error) as rawstring)
    ig.End;

fn init (name argc argv)
    ctx.live? = true
    ctx.name = name
    ctx.path = find-module-path project-dir name __env
    set-globals! (.. module-scope (globals))
    try
        'watch ctx.watcher ctx.path on-file-update
        main.run;
    except (ex)
        print "could not watch file:" ex
        1

do
    let init
    local-scope;
