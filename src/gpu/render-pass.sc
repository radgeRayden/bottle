using import struct
using import ..helpers
let wgpu = (import ..FFI.wgpu)

struct RenderPass
    _handle : wgpu.RenderPassEncoder
    _cmd-encoder : wgpu.CommandEncoder

    inline __typecall (cls cmd-encoder color-attachments)
        let handle =
            wgpu.CommandEncoderBeginRenderPass cmd-encoder
                &local wgpu.RenderPassDescriptor
                    label = "Bottle Render Pass"
                    colorAttachmentCount = (countof color-attachments)
                    colorAttachments =
                        &local color-attachments

        super-type.__typecall cls
            _handle = handle
            _cmd-encoder = cmd-encoder

do
    let RenderPass
    locals;
