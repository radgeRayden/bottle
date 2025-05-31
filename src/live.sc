using import Array radl.FileWatcher struct

#
# name argc argv := (script-launch-args)
# let filename =
#     if (argc > 0)
#         'from-rawstring String (argv @ 0)
#     else
#         error "no file selected"
#
# global fw : FileWatcher
# 'watch fw filename
#

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
            data = &var as voidstar
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

()
