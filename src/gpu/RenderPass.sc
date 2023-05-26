using import Array
using import glm
using import struct

using import .common
using import ..helpers
from (import .Texture) let TextureView
import .wgpu

type ColorAttachment < Struct :: (storageof wgpu.RenderPassColorAttachment)
    inline... __typecall (cls, view : TextureView, clear-color : vec4)
        let attachment =
            wgpu.RenderPassColorAttachment
                view = (storagecast view)
                loadOp = wgpu.LoadOp.Clear
                storeOp = wgpu.StoreOp.Store
                clearValue = wgpu.Color (unpack clear-color)
        bitcast attachment cls

type CommandBuffer < Struct :: (storageof wgpu.CommandBuffer)
    inline... __typecall (cls, value : wgpu.CommandBuffer)
        bitcast value cls

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
            _handle = handle
            _cmd-encoder = cmd-encoder

    fn finish (self)
        # TODO: fix lifetimes
        wgpu.RenderPassEncoderEnd (copy self._handle)
        cmd-buf := wgpu.CommandEncoderFinish (copy self._cmd-encoder) null
        CommandBuffer cmd-buf

do
    let ColorAttachment CommandBuffer RenderPass
    local-scope;
