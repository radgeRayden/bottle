using import enum

enum GPUError plain
    DiscardedFrame
    ObjectCreationFailed
    InvalidOperation
    InvalidInput

enum FilesystemError plain
    GenericError

do
    let FilesystemError
        GPUError
    locals;
