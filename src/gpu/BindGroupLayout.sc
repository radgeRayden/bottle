using import Array struct radl.ext
using import ..context .types
import .wgpu

ctx := context-accessor 'gpu

inline set-bit (dst mask value)
    if value
        dst |= mask
    else
        dst &= ~mask

type+ BindGroupLayout
    struct BindGroupLayoutBuilder
        entries : (Array wgpu.BindGroupLayoutEntry)
        visibility : wgpu.ShaderStageFlags

        fn... set-vertex-visibility (self, visible? : bool)
            set-bit self.visibility wgpu.ShaderStage.Vertex visible?

        fn... set-fragment-visibility (self, visible? : bool)
            set-bit self.visibility wgpu.ShaderStage.Fragment visible?

        fn... set-compute-visibility (self, visible? : bool)
            set-bit self.visibility wgpu.ShaderStage.Compute visible?

        fn clear-visibility (self)
            self.visibility = 0

        fn... add-buffer-binding (self, buffer-type : wgpu.BufferBindingType)
            'append self.entries
                wgpu.BindGroupLayoutEntry (binding = (countof self.entries) as u32)
                    visibility = self.visibility
                    buffer =
                        wgpu.BufferBindingLayout
                            type = buffer-type

        fn... add-sampler-binding (self, sampler-type : wgpu.SamplerBindingType = 'Filtering)
            'append self.entries
                wgpu.BindGroupLayoutEntry (binding = (countof self.entries) as u32)
                    visibility = self.visibility
                    sampler =
                        wgpu.SamplerBindingLayout
                            type = sampler-type

        fn... add-texture-binding (self, texture-sample-type : wgpu.TextureSampleType = 'Float,
                                    dimension : wgpu.TextureViewDimension = '2D,
                                    multisampled? : bool = false)
            'append self.entries
                wgpu.BindGroupLayoutEntry (binding = (countof self.entries) as u32)
                    visibility = self.visibility
                    texture =
                        wgpu.TextureBindingLayout
                            sampleType = texture-sample-type
                            viewDimension = dimension
                            multisampled = multisampled?

        fn finalize (self)
            using import .common

            ptr count := 'data self.entries
            wrap-nullable-object BindGroupLayout
                wgpu.DeviceCreateBindGroupLayout ctx.device
                    typeinit@
                        label = "bottle bind group layout"
                        entryCount = count
                        entries = dupe ptr

    inline builder (cls)
        (BindGroupLayoutBuilder)
