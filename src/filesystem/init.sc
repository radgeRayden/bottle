using import String
using import Array
using import print
using import Option
import physfs

using import ..config
cfg := cfg-accessor 'filesystem

using import ..exceptions

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
    assert (physfs.mount cfg.root "/" true) (get-error)

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

fn shutdown ()
    physfs.deinit;

do
    let init mount unmount shutdown realpath
    local-scope;
