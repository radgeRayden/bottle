using import Array radl.FileWatcher struct .context print radl.strfmt
import .main .callbacks .time

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

fn on-file-update (path ev-type)
    'clear module-storage
    if (ev-type == 'Modified)
        try
            reload-module;
            ()
        except (ex)
            print ('format ex)

@@ 'on callbacks.update
fn process-file-events (dt)
    if ctx.first-load?
        on-file-update ctx.path FileEventType.Modified
        ctx.first-load? = false
    else
        any-events? := 'dispatch-events ctx.watcher
        if any-events? (print f"source reloaded: ${ctx.name}. time: ${(time.get-raw-time)}")
    ()

fn init (name argc argv)
    ctx.live? = true
    ctx.name = name
    ctx.path = find-module-path project-dir name __env
    set-globals! (.. module-scope (globals))
    try
        'watch ctx.watcher ctx.path on-file-update
    except (ex)
        print "could not watch file:" ex
    main.run;

do
    let init
    local-scope;
