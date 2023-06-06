using import enum

enum GPUError plain
    OutdatedSwapchain
    ObjectCreationFailed
    InvalidOperation
    InvalidInput

enum FilesystemError plain
    GenericError

do
    let FilesystemError
        GPUError
    locals;
