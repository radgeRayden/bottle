import physfs

vvv bind API
do
    FileHandle := (mutable@ physfs.File)

    fn open-file (path mode)
        T := (typeof mode)
        switch mode
        case T.Read
            physfs.openRead path
        case T.Append
            physfs.openAppend path
        case T.Write
            physfs.openWrite path
        default (assert false "bad enum value")

    fn close-file (handle)
        physfs.close handle
        ()

    inline is-handle-invalid? (handle)
        handle == null

    fn get-file-length (handle)
        size := (physfs.fileLength handle)
        assert (size != -1)
        size as usize

    fn get-cursor-position (handle)
        (physfs.tell handle) as usize

    fn set-cursor-position (handle position)
        physfs.seek handle position
        ()

    fn read-bytes (handle ptr count)
        physfs.readBytes handle (ptr as voidstar) count

    fn write-bytes (handle ptr count)
        physfs.writeBytes handle (ptr as voidstar) count

    fn eof? (handle)
        (physfs.eof handle) as bool

    local-scope;

(import radl.IO.FileStream) API
