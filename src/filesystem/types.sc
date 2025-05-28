import physfs radl.traits
using import String struct radl.IO.FileStream

typedef PhysfsFile : (mutable@ physfs.File)
    using radl.traits.coerces-to-storage

do
    FileStream := (make-filestream-type PhysfsFile)
    let PhysfsFile
    local-scope;
