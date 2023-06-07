using import struct

using import ...gpu.types
using import .common

struct SpriteBatch
    attribute-buffer : (StorageBuffer VertexAttributes)

    fn flush (self)

do
    let SpriteBatch
    local-scope;
