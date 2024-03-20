using import Array property struct ..context ..exceptions ..helpers .types

import .wgpu
from wgpu let typeinit@ chained@

ctx := context-accessor 'gpu

fn make-buffer (size usage-flags)
    let handle =
        wgpu.DeviceCreateBuffer ctx.device
            &local wgpu.BufferDescriptor
                label = "Bottle buffer"
                usage = usage-flags
                size = (imply size u64)
    handle

fn write-buffer (buf data-ptr offset data-size)
    wgpu.QueueWriteBuffer ctx.queue
        buf
        offset
        data-ptr
        data-size

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

            using import .common
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

            write-buffer (view self) data-ptr byte-offset data-size
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

            write-buffer (view self) (&local data) byte-offset data-size
            ()
        # ------------------------------------------------------------------------------------

        fn get-byte-size (self)
            self.Capacity * ElementSize

        fn... clone (self, new-capacity : usize, copy-count : (param? usize) = none)
            new-buffer := (typeof self) new-capacity (wgpu.BufferGetUsage (view self))

            src-capacity := imply self.Capacity usize
            let copy-count =
                static-if (not (none? copy-count))
                    copy-count
                else
                    src-capacity

            assert (copy-count <= src-capacity)
            wgpu.CommandEncoderCopyBufferToBuffer \
                ctx.cmd-encoder self 0:u64 new-buffer 0:u64 (copy-count * ElementSize)
            new-buffer

        inline __imply (this other)
            static-if (other == wgpu.Buffer)
                inline (self)
                    bitcast self other

        unlet constructor

type+ GenericBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "GenericBuffer" backing-type

type+ StorageBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "StorageBuffer" backing-type
            wgpu.BufferUsage.Storage | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type+ IndexBuffer
    @@ memo
    inline __typecall (cls backing-type)
        static-if (not ((backing-type == u16) or (backing-type == u32)))
            hide-traceback;
            static-error "only u16 and u32 are allowed as index buffer backing types"

        gen-buffer-type cls "IndexBuffer" backing-type
            wgpu.BufferUsage.Index | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

type+ UniformBuffer
    @@ memo
    inline __typecall (cls backing-type)
        gen-buffer-type cls "UniformBuffer" backing-type
            wgpu.BufferUsage.Uniform | wgpu.BufferUsage.CopyDst | wgpu.BufferUsage.CopySrc

()
