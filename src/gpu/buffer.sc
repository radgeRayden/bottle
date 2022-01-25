using import struct
using import .istate
using import ..helpers
let wgpu = (import ..FFI.wgpu)

fn make-buffer (size)
    let handle =
        wgpu.DeviceCreateBuffer istate.device
            &local wgpu.BufferDescriptor
                label = "Bottle Storage buffer"
                usage = (wgpu.BufferUsage.Storage | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc)
                size = (imply size u64)

    handle


struct GPUBuffer
    handle : wgpu.Buffer
    bgroup : wgpu.BindGroup
    size : usize

    inline __typecall (cls size)
        let handle = (make-buffer size)
        let bgroup =
            wgpu.DeviceCreateBindGroup istate.device
                &local wgpu.BindGroupDescriptor
                    label = "bottle bind group"
                    layout = istate.default-bgroup-layout
                    entryCount = 1
                    entries =
                        &local wgpu.BindGroupEntry
                            binding = 0
                            buffer = handle
                            offset = 0
                            size = size

        super-type.__typecall cls
            handle = handle
            bgroup = bgroup
            size = size

    # currently you can only write all the data.
    # this is all pretty unsafe! Might add some more validation later.
    fn write (self data)
        let T = (typeof data)
        assert ((sizeof T.ElementType) * (countof data) >= self.size)
        wgpu.QueueWriteBuffer istate.queue self.handle 0 ((imply data pointer) as voidstar) self.size

    inline __drop (self)
        wgpu.BufferDrop self.handle

do
    let GPUBuffer
    locals;
