using import String
import physfs

fn init ()
    assert (physfs.init null) "Filesystem could not be initialized."
    assert (physfs.mount "." "/" true)

fn shutdown ()
    physfs.deinit;
do
    let init shutdown
    local-scope;
