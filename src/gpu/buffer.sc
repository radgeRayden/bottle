using import struct
using import .common
using import ..helpers
import wgpu

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
    size : usize

    # FIXME: makes no sense that this needs the layout.
    # We won't be creating bind groups inside buffer in the future.
    inline __typecall (cls size)
        let handle = (make-buffer size)

        super-type.__typecall cls
            handle = handle
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
