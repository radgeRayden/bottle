using import String
using import Array
import physfs

using import ..exceptions

fn... check-error (result, failure-code = 0)
    let code =
        static-if ((superof result) == pointer)
            ptrtoint result u64
        else result

    if (code == (failure-code as (typeof code)))
        err := (physfs.getLastErrorCode)
        print "Filesystem error:" (String (physfs.getErrorByCode err))
        raise FilesystemError.GenericError
    result

fn init ()
    assert (physfs.init null)
    assert (physfs.mount "." "/" true)

fn mount (dir mount-point)
    check-error
        physfs.mount dir mount-point true

fn load-file (filename)
    handle := check-error (physfs.openRead filename)
    local buf : (Array u8)
    len := check-error (physfs.fileLength handle) -1
    'resize buf len
    check-error (physfs.readBytes handle ((imply buf pointer) as voidstar) (len as u64)) -1
    buf

fn shutdown ()
    physfs.deinit;

do
    let init mount shutdown load-file
    local-scope;
