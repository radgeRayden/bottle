using import .common .types
import .wgpu

type+ CommandBuffer
    fn submit (self)
        local self = (storagecast self)
        wgpu.QueueSubmit istate.queue 1 &self

type+ CommandEncoder
    inline __typecall (cls)
        wrap-nullable-object cls
            wgpu.DeviceCreateCommandEncoder istate.device (&local wgpu.CommandEncoderDescriptor)

    fn finish (self)
        cmd-buf := wgpu.CommandEncoderFinish (view self) null
        # gets transmogrified into CommandBuffer, so no need to drop
        lose self
        imply cmd-buf CommandBuffer

()
