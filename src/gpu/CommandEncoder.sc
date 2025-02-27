using import ..context glm .types radl.ext
import .wgpu

ctx := context-accessor 'gpu

type+ CommandBuffer
    fn submit (self)
        local self = (storagecast self)
        wgpu.QueueSubmit ctx.queue 1 &self

type+ CommandEncoder

    fn finish (self)
        cmd-buf := wgpu.CommandEncoderFinish (view self)
            typeinit@
                label = "Command Buffer"
        imply cmd-buf CommandBuffer

    fn... copy-buffer (self, source : GPUBuffer, src-offset : usize,
                        destination : GPUBuffer, dst-offset : usize, count : usize)
        srcT dstT := typeof source, typeof destination
        static-if (not (imply? srcT.BackingType dstT.BackingType))
            static-error "Incompatible buffer types"

        copy-size := srcT.ElementSize * count
        assert ((src-offset + copy-size) <= source.CapacityBytes)
        assert ((dst-offset + copy-size) <= destination.CapacityBytes)

        wgpu.CommandEncoderCopyBufferToBuffer self source src-offset destination dst-offset copy-size
    # TODO: implement buffer to texture

    fn... copy-texture (self, source : Texture, src-mip-level : u32, src-origin : uvec3, src-aspect : wgpu.TextureAspect = 'All,
                         destination : Texture, dst-mip-level : u32, dst-origin : uvec3, dst-aspect : wgpu.TextureAspect = 'All,
                         copy-extent : uvec3)
        wgpu.CommandEncoderCopyTextureToTexture self
            typeinit@
                texture = (view source)
                mipLevel = src-mip-level
                origin = (typeinit (unpack src-origin))
                aspect = src-aspect
            typeinit@
                texture = (view destination)
                mipLevel = dst-mip-level
                origin = (typeinit (unpack dst-origin))
                aspect = dst-aspect
            typeinit@ (unpack copy-extent)
()
