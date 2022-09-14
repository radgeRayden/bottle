using import struct
using import ..helpers
using import .common
using import .buffer
import wgpu

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

    fn set-bindgroup (self binding group)
        wgpu.RenderPassEncoderSetBindGroup self._handle binding group._handle 0 null

    fn... set-index-buffer (self, ibuffer : GPUIndexBuffer)
        let format =
            static-match ((typeof ibuffer) . BackingType)
            case u16
                wgpu.IndexFormat.Uint16
            case u32
                wgpu.IndexFormat.Uint32
            default
                static-error "invalid index buffer type"

        wgpu.RenderPassEncoderSetIndexBuffer self._handle ibuffer._handle format 0 ibuffer._size

    fn set-bindings (self bindings...)
        wgpu.RenderPassEncoderSetBindGroup self._handle 0
            'get-bind-group istate bindings...
            0
            null

    fn draw (self vertex-count instance-count first-vertex first-instance)
        wgpu.RenderPassEncoderDraw self._handle vertex-count instance-count first-vertex first-instance

    fn draw-indexed (self index-count instance-count first-index first-instance)
        wgpu.RenderPassEncoderDrawIndexed self._handle index-count instance-count first-index 0:u32 first-instance

    fn finish (self)
        wgpu.RenderPassEncoderEnd self._handle

do
    let RenderPass
    locals;
