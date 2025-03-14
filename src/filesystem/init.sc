using import Array Buffer Option slice String print
import C.bindings physfs

using import ..context
cfg := context-accessor 'config 'filesystem

using import ..exceptions

inline getcwd (ptr size)
    static-if (operating-system == 'windows)
        (extern '_getcwd (function rawstring rawstring i32)) ptr (size as i32)
    else 
        C.bindings.extern.getcwd ptr size

for k v in physfs
    if (('typeof v) != type)
        continue;

    local old-symbols : (Array Symbol)
    T := (v as type)
    if (T < CEnum)
        for k v in ('symbols T)
            original-symbol  := k as Symbol
            original-name    := original-symbol as string
            match? start end := 'match? str"^PHYSFS_" original-name

            if match?
                field := (Symbol (rslice original-name end))
                'set-symbol T field v
                'append old-symbols original-symbol

        for sym in old-symbols
            sc_type_del_symbol T sym
run-stage;

fn get-error ()
    err := (physfs.getLastErrorCode)
    msg := (physfs.getErrorByCode err)
    .. "Filesystem error: " ('from-rawstring String msg)

fn... check-error (result, failure-code = 0)
    let code =
        static-if ((superof result) == pointer)
            ptrtoint result u64
        else result

    if (code == (failure-code as (typeof code)))
        print (get-error)
        raise FilesystemError.GenericError
    result

fn init ()
    assert (physfs.init null) (get-error)

    let root =
        try (copy ('unwrap cfg.root))
        else
            buf := stringbuffer i8 260
            assert ((getcwd ('data buf)) != null)
            String ('data buf)

    assert (physfs.mount root "/" true) (get-error)

    try
        save-dir := 'unwrap cfg.save-directory
        assert (physfs.mountRW save-dir "save-directory" true)
    else ()

fn mount (path mount-point allow-writing?)
    check-error
        if allow-writing?
            physfs.mountRW path mount-point true
        else
            physfs.mount path mount-point true

fn unmount (path)
    physfs.unmount path

fn realpath (path)
    viewing path
    dir := physfs.getRealDir path
    if (dir != null)
        (Option String)
            .. ('from-rawstring String dir) path
    else
        ((Option String))

fn... get-directory-items (dir : String)
    local result : (Array String)
    file-list := physfs.enumerateFiles dir
    if (file-list != null)
        loop (next = file-list)
            this := @next
            if (this == null)
                break;

            'append result ('from-rawstring String this)
            & (next @ 1)
    result

fn is-directory? (path)
    local stat : physfs.Stat
    result := physfs.stat path &stat
    if (not result)
        false
    else
        stat.filetype == physfs.FileType.FILETYPE_DIRECTORY

fn shutdown ()
    physfs.deinit;

do
    let init mount unmount get-directory-items is-directory? shutdown realpath
    local-scope;
