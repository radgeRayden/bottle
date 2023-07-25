import .wgpu
using import .common
using import .types

type+ Sampler
    inline __typecall (cls)
        wrap-nullable-object cls
            wgpu.DeviceCreateSampler istate.device null # TODO: configuration

()
