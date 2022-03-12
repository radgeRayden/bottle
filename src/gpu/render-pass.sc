using import struct
using import ..helpers
using import .common
let wgpu = (import ..FFI.wgpu)

struct RenderPass
    _handle : wgpu.RenderPassEncoder
    _cmd-encoder : wgpu.CommandEncoder
    _pipeline : wgpu.RenderPipeline

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

    fn set-pipeline (self pipeline)
        wgpu.RenderPassEncoderSetPipeline self._handle pipeline._handle

    fn set-bindings (self bindings...)
        wgpu.RenderPassEncoderSetBindGroup self._handle 0
            'get-bind-group istate bindings...
            0
            null

    fn draw (self vertex-count instance-count first-vertex first-instance)
        wgpu.RenderPassEncoderDraw self._handle vertex-count instance-count first-vertex first-instance

    fn finish (self)
        wgpu.RenderPassEncoderEndPass self._handle

do
    let RenderPass
    locals;
