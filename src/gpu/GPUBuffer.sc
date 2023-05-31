using import Array
using import struct
using import .common
using import ..helpers
import .wgpu

fn make-buffer (size usage-flags)
    let handle =
        wgpu.DeviceCreateBuffer istate.device
            &local wgpu.BufferDescriptor
                label = "Bottle buffer"
                usage = usage-flags
                size = (imply size u64)
    handle

fn write-buffer (buf data-ptr offset data-size)
    wgpu.QueueWriteBuffer istate.queue
        buf
        offset
        data-ptr
        data-size

struct GPUBuffer
    _handle   : wgpu.Buffer
    _capacity : usize
    _usage    : wgpu.BufferUsage

@@ memo
inline gen-buffer-type (parent-type prefix backing-type usage-flags)
    type (.. prefix "<" (tostring backing-type) ">") < parent-type :: (storageof GPUBuffer)
        BackingType := backing-type
        ElementSize := (sizeof BackingType)

        inline constructor (cls max-elements usage-flags)
            # TODO: ensure size obeys alignment rules
            size   := max-elements * (sizeof BackingType)
            handle := make-buffer size usage-flags

            # use Struct directly to avoid hierarchy issues
            Struct.__typecall cls
                _handle = (wrap-nullable-object wgpu.Buffer handle)
                _capacity = max-elements
                _usage = usage-flags

        # if usage flags aren't statically provided, it means they must be passed at runtime
        let __typecall =
            static-if (none? usage-flags)
                inline __typecall (cls max-elements usage-flags)
                    constructor cls max-elements usage-flags
            else
                inline __typecall (cls max-elements)
                    constructor cls max-elements usage-flags

        # ------------------------------------------------------------------------------------
        fn... frame-write (self, data : (Array BackingType), offset : usize, count : usize)
            data-size     := ElementSize * count
            byte-offset   := ElementSize * offset
            byte-capacity := ElementSize * self._capacity
            data-ptr      := (imply data pointer) as voidstar

            assert ((byte-offset + data-size) <= byte-capacity)
            write-buffer self._handle data-ptr offset data-size
            ()

        case (self, data : (Array BackingType), offset : usize)
            this-function self data offset (countof data)

        case (self, data : (Array BackingType))
            this-function self data 0 (countof data)

        case (self, data : BackingType, offset = 0:usize)
            data-size     := ElementSize
            byte-offset   := ElementSize * offset
            byte-capacity := ElementSize * self._capacity

            assert ((byte-offset + data-size) <= byte-capacity)
            write-buffer self._handle (&local data) offset data-size
            ()
        # ------------------------------------------------------------------------------------

        inline __imply (this other)
            static-if (other == (storageof wgpu.Buffer))
                inline (self)
                    imply self._handle other

        unlet constructor

type GenericBuffer < GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "GenericBuffer" backing-type

type StorageBuffer < GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "StorageBuffer" backing-type
            wgpu.BufferUsage.Storage | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type IndexBuffer < GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        static-if (not ((backing-type == u16) or (backing-type == u32)))
            hide-traceback;
            static-error "only u16 and u32 are allowed as index buffer backing types"

        gen-buffer-type cls "IndexBuffer" backing-type
            wgpu.BufferUsage.Index | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type UniformBuffer < GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "UniformBuffer" backing-type
            wgpu.BufferUsage.Uniform | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

do

    let StorageBuffer IndexBuffer UniformBuffer GenericBuffer
    local-scope;
