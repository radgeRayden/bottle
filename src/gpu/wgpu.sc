using import Array
using import String
import wgpu

LOSE-ENCODERS-ON-FINISH := false

inline wrap-constructor (f T)
    inline (...)
        bitcast
            f ...
            T

inline define-object (name super destructor)
    type (_ name) < Struct :: super
        inline __typecall (cls)
            bitcast null cls

        inline __drop (self)
            if ((storagecast self) != null)
                destructor (bitcast self super)

        inline __rimply (otherT thisT)
            static-if (otherT == super)
                inline (incoming)
                    bitcast incoming thisT
            elseif (otherT == (superof thisT))
                inline (incoming)
                    bitcast incoming thisT

        inline __imply (thisT otherT)
            static-if (otherT == super)
                inline (self)
                    bitcast self otherT
            elseif (otherT == (superof thisT))
                inline (incoming)
                    bitcast incoming thisT

        inline __hash (self)
            hash (storagecast (view self))

        inline get-id (self)
            ptrtoint (storagecast (view self)) u64

inline define-flags (enumT)
    inline __typecall (cls flags...)
        flagST := storageof wgpu.Flags

        result :=
            static-fold (result = (flagST)) for flag in (va-each flags...)
                result | ((getattr enumT flag) as flagST)

        bitcast result cls

fn CEnum->String (T)
    T as:= type
    inline gen-switch (value)
        local tags : (Array (tuple (value = u64) (name = string)))
        sw := (sc_switch_new value)
        for k v in ('symbols T)
            if (('typeof v) < CEnum)
                'append tags
                    typeinit
                        value = (sc_const_int_extract v)
                        name = k as Symbol as string
        'sort tags

        # deduplicate
        fold (prev-value prev-name = -1:u64 str"") for i in (rrange (countof tags))
            t := tags @ i
            value name := (deref t.value), (deref t.name)

            let name =
                if (t.value == prev-value)
                    'remove tags (i + 1)
                    .. name "|" prev-name
                else
                    name

            t.name = name

            _ value name

        for t in tags
            sc_switch_append_case sw (sc_const_int_new T t.value)
                spice-quote
                    String [t.name]

        sc_switch_append_default sw
            spice-quote
                String "?invalid?"
        sw

    spice-quote
        fn (value)
            [(gen-switch value)]

for scope in ('lineage wgpu)
    for k v in scope
        vvv bind T
        if (('typeof v) == type)
            v as type
        else
            continue;

        if (T < CEnum)
            'set-symbol T '__tostring
                CEnum->String T

run-stage;

do
    Instance := define-object "WGPUInstance" wgpu.Instance wgpu.InstanceDrop
    Adapter := define-object "WGPUAdapter" wgpu.Adapter
    BindGroup := define-object "WGPUBindGroup" wgpu.BindGroup wgpu.BindGroupDrop
    BindGroupLayout := define-object "WGPUBindGroupLayout" wgpu.BindGroupLayout wgpu.BindGroupLayoutDrop
    Buffer := define-object "WGPUBuffer" wgpu.Buffer wgpu.BufferDrop wgpu.DeviceCreateBuffer
    CommandBuffer := define-object "WGPUCommandBuffer" wgpu.CommandBuffer wgpu.CommandBufferDrop
    CommandEncoder := define-object "WGPUCommandEncoder" wgpu.CommandEncoder wgpu.CommandEncoderDrop
    RenderPassEncoder := define-object "WGPURenderPassEncoder" wgpu.RenderPassEncoder wgpu.RenderPassEncoderDrop
    ComputePassEncoder := define-object "WGPUComputePassEncoder" wgpu.ComputePassEncoder wgpu.ComputePassEncoderDrop
    RenderBundleEncoder := define-object "WGPURenderBundleEncoder" wgpu.RenderBundleEncoder wgpu.RenderBundleEncoderDrop
    ComputePipeline := define-object "WGPUComputePipeline" wgpu.ComputePipeline wgpu.ComputePipelineDrop
    Device := define-object "WGPUDevice" wgpu.Device
    PipelineLayout := define-object "WGPUPipelineLayout" wgpu.PipelineLayout wgpu.PipelineLayoutDrop
    QuerySet := define-object "WGPUQuerySet" wgpu.QuerySet wgpu.QuerySetDrop
    RenderBundle := define-object "WGPURenderBundle" wgpu.RenderBundle wgpu.RenderBundleDrop
    RenderPipeline := define-object "WGPURenderPipeline" wgpu.RenderPipeline wgpu.RenderPipelineDrop
    Sampler := define-object "WGPUSampler" wgpu.Sampler wgpu.SamplerDrop
    ShaderModule := define-object "WGPUShaderModule" wgpu.ShaderModule wgpu.ShaderModuleDrop
    Surface := define-object "WGPUSurface" wgpu.Surface wgpu.SurfaceDrop
    SwapChain := define-object "WGPUSwapChain" wgpu.SwapChain wgpu.SwapChainDrop
    Texture := define-object "WGPUTexture" wgpu.Texture wgpu.TextureDrop
    TextureView := define-object "WGPUTextureView" wgpu.TextureView wgpu.TextureViewDrop

    TextureCreateView := wrap-constructor wgpu.TextureCreateView TextureView
    DeviceCreateTexture := wrap-constructor wgpu.DeviceCreateTexture Texture
    DeviceCreateSwapChain := wrap-constructor wgpu.DeviceCreateSwapChain SwapChain
    InstanceCreateSurface := wrap-constructor wgpu.InstanceCreateSurface Surface
    DeviceCreateShaderModule := wrap-constructor wgpu.DeviceCreateShaderModule ShaderModule
    DeviceCreateSampler := wrap-constructor wgpu.DeviceCreateSampler Sampler
    DeviceCreateRenderPipeline := wrap-constructor wgpu.DeviceCreateRenderPipeline RenderPipeline
    RenderBundleEncoderFinish := wrap-constructor wgpu.RenderBundleEncoderFinish RenderBundle
    DeviceCreateQuerySet := wrap-constructor wgpu.DeviceCreateQuerySet QuerySet
    DeviceCreatePipelineLayout := wrap-constructor wgpu.DeviceCreatePipelineLayout PipelineLayout
    DeviceDrop := wrap-constructor wgpu.DeviceDrop Device
    DeviceCreateComputePipeline := wrap-constructor wgpu.DeviceCreateComputePipeline ComputePipeline
    DeviceCreateRenderBundleEncoder := wrap-constructor wgpu.DeviceCreateRenderBundleEncoder RenderBundleEncoder
    CommandEncoderBeginComputePass := wrap-constructor wgpu.CommandEncoderBeginComputePass ComputePassEncoder
    CommandEncoderBeginRenderPass := wrap-constructor wgpu.CommandEncoderBeginRenderPass RenderPassEncoder
    DeviceCreateCommandEncoder := wrap-constructor wgpu.DeviceCreateCommandEncoder CommandEncoder
    DeviceCreateBuffer := wrap-constructor wgpu.DeviceCreateBuffer Buffer
    DeviceCreateBindGroupLayout := wrap-constructor wgpu.DeviceCreateBindGroupLayout BindGroupLayout
    DeviceCreateBindGroup := wrap-constructor wgpu.DeviceCreateBindGroup BindGroup
    AdapterDrop := wrap-constructor wgpu.AdapterDrop Adapter
    CreateInstance := wrap-constructor wgpu.CreateInstance Instance

    # special cases that transform input
    inline CommandEncoderFinish (cmd-encoder desc)
        result :=
            wgpu.CommandEncoderFinish cmd-encoder desc
        static-if LOSE-ENCODERS-ON-FINISH
            lose cmd-encoder
        imply result CommandBuffer

    inline RenderPassEncoderEnd (render-pass)
        wgpu.RenderPassEncoderEnd render-pass
        static-if LOSE-ENCODERS-ON-FINISH
            lose render-pass

    inline QueueSubmit (queue count cmd-buffers)
        wgpu.QueueSubmit queue count cmd-buffers
        lose cmd-buffers

    type ShaderStageFlags < integer : u32
        let __typecall = (define-flags wgpu.ShaderStage)

    .. (local-scope) wgpu
