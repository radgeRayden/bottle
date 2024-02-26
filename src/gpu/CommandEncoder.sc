using import ..context .types
import .wgpu

ctx := context-accessor 'gpu

type+ CommandBuffer
    fn submit (self)
        local self = (storagecast self)
        wgpu.QueueSubmit ctx.queue 1 &self

type+ CommandEncoder
    inline __typecall (cls)
        using import .common
        wrap-nullable-object cls
            wgpu.DeviceCreateCommandEncoder ctx.device (&local wgpu.CommandEncoderDescriptor)

    fn finish (self)
        cmd-buf := wgpu.CommandEncoderFinish (view self) null
        # gets transmogrified into CommandBuffer, so no need to drop
        lose self
        imply cmd-buf CommandBuffer

()
