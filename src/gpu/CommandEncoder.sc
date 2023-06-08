using import .common
import .wgpu

type CommandBuffer <:: wgpu.CommandBuffer
    fn submit (self)
        local self = (storagecast self)
        wgpu.QueueSubmit istate.queue 1 &self

type CommandEncoder <:: wgpu.CommandEncoder
    inline __typecall (cls)
        wrap-nullable-object cls
            wgpu.DeviceCreateCommandEncoder istate.device (&local wgpu.CommandEncoderDescriptor)

    fn finish (self)
        cmd-buf := wgpu.CommandEncoderFinish (view self) null
        # gets transmogrified into CommandBuffer, so no need to drop
        lose self
        imply cmd-buf CommandBuffer

do
    let CommandBuffer CommandEncoder
    local-scope;
