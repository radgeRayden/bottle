using import Array
using import struct
using import .common
using import ..helpers
import wgpu

fn make-buffer (size usage-flags)
    let handle =
        wgpu.DeviceCreateBuffer istate.device
            &local wgpu.BufferDescriptor
                label = "Bottle Storage buffer"
                usage = usage-flags
                size = (imply size u64)
    handle

type GPUBuffer < Struct

@@ memo
inline gen-buffer-type (prefix backing-type usage-flags)
    struct (.. prefix "<" (tostring backing-type) ">") < GPUBuffer
        _handle : wgpu.Buffer
        _size : usize
        _usage : wgpu.BufferUsage

        let BackingType = backing-type

        inline constructor (cls max-elements usage-flags)
            size   := max-elements * (sizeof BackingType)
            handle := (make-buffer size usage-flags)

            super-type.__typecall cls
                _handle = handle
                _size = size

        # if usage flags aren't statically provided, it means they must be passed at runtime
        let __typecall =
            static-if (none? usage-flags)
                inline __typecall (cls max-elements usage-flags)
                    constructor cls max-elements usage-flags
            else
                inline __typecall (cls max-elements)
                    constructor cls max-elements usage-flags

        fn... write (self, data : (Array backing-type))
            data-size := (sizeof ((typeof data) . ElementType)) * (countof data)
            assert (data-size <= self._size)
            wgpu.QueueWriteBuffer istate.queue self._handle 0 ((imply data pointer) as voidstar) self._size

        inline __drop (self)
            wgpu.BufferDrop self._handle

        unlet constructor

type GPUGenericBuffer < GPUBuffer
    inline __typecall (cls backing-type)
        gen-buffer-type "GPUBuffer" backing-type

type GPUStorageBuffer < GPUBuffer
    inline __typecall (cls backing-type)
        gen-buffer-type "GPUStorageBuffer" backing-type
            wgpu.BufferUsage.Storage | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type GPUIndexBuffer < GPUBuffer
    inline __typecall (cls backing-type)
        gen-buffer-type "GPUIndexBuffer" backing-type
            wgpu.BufferUsage.Index | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type GPUUniformBuffer < GPUBuffer
    inline __typecall (cls backing-type)
        gen-buffer-type "GPUUniformBuffer" backing-type
            wgpu.BufferUsage.Uniform | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

do
    let GPUBuffer = GPUGenericBuffer
    let GPUStorageBuffer GPUIndexBuffer GPUUniformBuffer
    locals;
