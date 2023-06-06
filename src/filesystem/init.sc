using import String
import physfs

using import ..exceptions

fn check-error (result fatal?)
    if (result == 0)
        err := (physfs.getLastErrorCode)
        print "Filesystem error:" (String (physfs.getErrorByCode err))
        if fatal? (abort)
        else
            raise FilesystemError.GenericError

fn init ()
    check-error (physfs.init null) true
    check-error (physfs.mount "." "/" true) true

fn mount (dir mount-point)
    check-error
        physfs.mount dir mount-point true

fn shutdown ()
    physfs.deinit;
do
    let init shutdown
    local-scope;
