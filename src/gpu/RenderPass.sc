using import Array
using import glm
using import struct

using import .common
using import ..helpers
using import .BindGroup
using import .GPUBuffer
from (import .Texture) let TextureView
using import .RenderPipeline
import .wgpu

type ColorAttachment <: wgpu.RenderPassColorAttachment
    inline... __typecall (cls, view : TextureView, clear-color : vec4)
        let attachment =
            wgpu.RenderPassColorAttachment
                view = (storagecast view)
                loadOp = wgpu.LoadOp.Clear
                storeOp = wgpu.StoreOp.Store
                clearValue = wgpu.Color (unpack clear-color)
        bitcast attachment cls

type CommandBuffer <:: wgpu.CommandBuffer
    fn submit (self)
        local self = (storagecast self)
        wgpu.QueueSubmit istate.queue 1 &self

struct RenderPass
    _handle : wgpu.RenderPassEncoder
    _cmd-encoder : wgpu.CommandEncoder

    inline... __typecall (cls, color-attachments)
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

        cmd-encoder := wgpu.DeviceCreateCommandEncoder istate.device (&local wgpu.CommandEncoderDescriptor)

        let handle =
            wgpu.CommandEncoderBeginRenderPass cmd-encoder
                &local wgpu.RenderPassDescriptor
                    label = "Bottle Render Pass"
                    colorAttachmentCount = count
                    colorAttachments = color-attachments as (@ wgpu.RenderPassColorAttachment)

        super-type.__typecall cls
            _handle = (wrap-nullable-object wgpu.RenderPassEncoder handle)
            _cmd-encoder = cmd-encoder

    inline __imply (this other)
        static-if (imply? wgpu.RenderPassEncoder other)
            inline (self)
                self._handle
        elseif (imply? wgpu.CommandEncoder other)
            inline (self)
                self._cmd-encoder

    inline finish (self)
        wgpu.RenderPassEncoderEnd self._handle
        cmd-buf := wgpu.CommandEncoderFinish self._cmd-encoder null
        # gets transmogrified into CommandBuffer, so no need to drop
        lose self
        imply cmd-buf CommandBuffer

    fn... set-pipeline (self, pipeline : RenderPipeline)
        wgpu.RenderPassEncoderSetPipeline (view self) (view pipeline)

    fn... set-index-buffer (self, buffer : IndexBuffer, offset = 0:usize, count...)
        count := va-option count count... (buffer.Capacity - offset)
        # TODO: error instead of assert
        assert (offset < buffer.Capacity)

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
        wgpu.RenderPassEncoderDraw (view self._handle) ...

    fn... draw-indexed (self, index-count : u32, instance-count = 1:u32, first-index = 0:u32, base-vertex = 0:i32, first-instance = 0:u32)
        self ... := *...
        wgpu.RenderPassEncoderDrawIndexed (view self._handle) ...

do
    let ColorAttachment CommandBuffer RenderPass
    local-scope;
