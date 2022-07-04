using import Array
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


type GPUBuffer < Struct
    @@ memo
    inline __typecall (cls T)
        struct (.. "GPUBuffer<" (tostring T) ">") < this-type
            _handle : wgpu.Buffer
            _size : usize

            inline __typecall (cls max-elements)
                size   := max-elements * (sizeof T)
                handle := (make-buffer size)

                Struct.__typecall cls
                    _handle = handle
                    _size = size

            fn... write (self, data : (Array T))
                data-size := (sizeof ((typeof data) . ElementType)) * (countof data)
                assert (data-size <= self._size)
                wgpu.QueueWriteBuffer istate.queue self._handle 0 ((imply data pointer) as voidstar) self._size

            inline __drop (self)
                wgpu.BufferDrop self._handle

do
    let GPUBuffer
    locals;
