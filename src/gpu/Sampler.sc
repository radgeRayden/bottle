import .wgpu
using import .common ..context .types radl.ext

ctx := context-accessor 'gpu

type+ Sampler
    inline... __typecall (cls, wrap-mode : wgpu.AddressMode = wgpu.AddressMode.ClampToEdge,
        filter-mode : wgpu.FilterMode = wgpu.FilterMode.Linear)

        wrap-nullable-object cls
            wgpu.DeviceCreateSampler ctx.device
                typeinit@
                    addressModeU = wrap-mode
                    addressModeV = wrap-mode
                    addressModeW = wrap-mode
                    magFilter = filter-mode
                    minFilter = filter-mode
                    lodMinClamp = 0.0
                    lodMaxClamp = 32.0
                    mipmapFilter = (bitcast filter-mode wgpu.MipmapFilterMode)
                    maxAnisotropy = 1
                    # FIXME: incomplete, inflexible

()
