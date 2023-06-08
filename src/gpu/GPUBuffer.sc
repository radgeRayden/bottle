using import Array
using import property
using import struct
using import .common
using import .CommandEncoder
using import ..exceptions
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

typedef GPUBuffer <:: wgpu.Buffer

@@ memo
inline gen-buffer-type (parent-type prefix backing-type usage-flags)
    type (.. prefix "<" (tostring backing-type) ">") <:: parent-type
        BackingType := backing-type
        ElementSize := (sizeof BackingType)

        Capacity :=
            property
                inline "getter" (self)
                    (wgpu.BufferGetSize (view self)) // ElementSize

        inline constructor (cls max-elements usage-flags)
            # TODO: ensure size obeys alignment rules
            size   := max-elements * (sizeof BackingType)
            handle := make-buffer size usage-flags

            bitcast
                wrap-nullable-object wgpu.Buffer handle
                cls

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
            byte-capacity := ElementSize * self.Capacity
            data-ptr      := (imply data pointer) as voidstar

            if ((byte-offset + data-size) > byte-capacity)
                raise GPUError.InvalidInput

            write-buffer (view self) data-ptr offset data-size
            ()

        case (self, data : (Array BackingType), offset : usize)
            this-function self data offset (countof data)

        case (self, data : (Array BackingType))
            this-function self data 0 (countof data)

        case (self, data : BackingType, offset = 0:usize)
            data-size     := ElementSize
            byte-offset   := ElementSize * offset
            byte-capacity := ElementSize * self.Capacity

            if ((byte-offset + data-size) > byte-capacity)
                raise GPUError.InvalidInput

            write-buffer (view self) (&local data) offset data-size
            ()
        # ------------------------------------------------------------------------------------

        fn get-byte-size (self)
            self.Capacity * ElementSize

        fn... clone (self, new-capacity : usize)
            new-buffer := (typeof self) new-capacity (wgpu.BufferGetUsage (view self))
            cmd-encoder := imply ('force-unwrap istate.cmd-encoder) CommandEncoder
            wgpu.CommandEncoderCopyBufferToBuffer \
                cmd-encoder self 0:u64 new-buffer 0:u64 ('get-byte-size self)
            new-buffer

        inline __imply (this other)
            static-if (other == wgpu.Buffer)
                inline (self)
                    bitcast self other

        unlet constructor

type GenericBuffer <:: GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "GenericBuffer" backing-type

type StorageBuffer <:: GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "StorageBuffer" backing-type
            wgpu.BufferUsage.Storage | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type IndexBuffer <:: GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        static-if (not ((backing-type == u16) or (backing-type == u32)))
            hide-traceback;
            static-error "only u16 and u32 are allowed as index buffer backing types"

        gen-buffer-type cls "IndexBuffer" backing-type
            wgpu.BufferUsage.Index | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type UniformBuffer <:: GPUBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "UniformBuffer" backing-type
            wgpu.BufferUsage.Uniform | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

do

    let StorageBuffer IndexBuffer UniformBuffer GenericBuffer GPUBuffer
    local-scope;
