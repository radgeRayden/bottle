import .wgpu
using import .common

type GPUSampler <:: wgpu.Sampler
    inline __typecall (cls)
        wrap-nullable-object cls
            wgpu.DeviceCreateSampler istate.device null # TODO: configuration

do
    let Sampler = GPUSampler # aliased due to glsl Sampler type
    local-scope;
