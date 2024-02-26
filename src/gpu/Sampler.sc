import .wgpu
using import .common ..context .types

ctx := context-accessor 'gpu

type+ Sampler
    inline __typecall (cls)
        wrap-nullable-object cls
            wgpu.DeviceCreateSampler ctx.device null # TODO: configuration
()
