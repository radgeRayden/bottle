using import Array
using import glm
using import struct

using import .common
using import ..exceptions
using import ..helpers
using import .BindGroup
using import .GPUBuffer
from (import .Texture) let TextureView
using import .RenderPipeline
import .wgpu

type ColorAttachment <: wgpu.RenderPassColorAttachment
    inline... __typecall (cls, view : TextureView, resolve-target : TextureView = null, clear? = true, clear-color = (vec4))
        let attachment =
            wgpu.RenderPassColorAttachment
                view = view
                resolveTarget = resolve-target
                loadOp = (clear? wgpu.LoadOp.Clear wgpu.LoadOp.Load)
                storeOp = wgpu.StoreOp.Store
                clearValue = wgpu.Color (unpack clear-color)
        bitcast attachment cls

type RenderPass <:: wgpu.RenderPassEncoder
    inline... __typecall (cls, cmd-encoder, color-attachments)
        vvv bind color-attachments count
        static-match (typeof color-attachments)
        case (Array ColorAttachment)
            _
                imply color-attachments pointer
                countof color-attachments
        case (array ColorAttachment)
            local attachments = color-attachments
            _
                deref &attachments
                countof attachments
        case ColorAttachment
            local attachment = color-attachments
            _
                deref &attachment
                1
        default
            static-error "wrong type for color-attachments"

        let handle =
            wgpu.CommandEncoderBeginRenderPass cmd-encoder
                &local wgpu.RenderPassDescriptor
                    label = "Bottle Render Pass"
                    colorAttachmentCount = count
                    colorAttachments = color-attachments as (@ wgpu.RenderPassColorAttachment)

        wrap-nullable-object cls handle

    inline finish (self)
        wgpu.RenderPassEncoderEnd self
        lose self

    fn... set-pipeline (self, pipeline : RenderPipeline)
        wgpu.RenderPassEncoderSetPipeline (view self) (view pipeline)

    fn... set-index-buffer (self, buffer : IndexBuffer, offset = 0:usize, count...)
        count := va-option count count... (buffer.Capacity - offset)
        if (offset >= buffer.Capacity)
            raise GPUError.InvalidInput

        indexT := (typeof buffer) . BackingType
        offset := (sizeof indexT) * offset
        size   := (sizeof indexT) * count

        let index-format =
            static-match indexT
            case u16
                wgpu.IndexFormat.Uint16
            case u32
                wgpu.IndexFormat.Uint32
            default
                wgpu.IndexFormat.Undefined

        wgpu.RenderPassEncoderSetIndexBuffer (view self) (view buffer) index-format offset size

    fn... set-bind-group (self, slot : u32, bind-group : BindGroup)
        # TODO: support dynamic offsets
        wgpu.RenderPassEncoderSetBindGroup (view self) slot bind-group 0 null

    fn... draw (self, vertex-count : u32, instance-count = 1:u32, first-vertex = 0:u32, first-instance = 0:u32)
        self ... := *...
        wgpu.RenderPassEncoderDraw (view self) ...

    fn... draw-indexed (self, index-count : u32, instance-count = 1:u32, first-index = 0:u32, base-vertex = 0:i32, first-instance = 0:u32)
        self ... := *...
        wgpu.RenderPassEncoderDrawIndexed (view self) ...

do
    let ColorAttachment RenderPass
    local-scope;
