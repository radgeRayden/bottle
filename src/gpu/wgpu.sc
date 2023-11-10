using import Array
using import hash
using import String
import wgpu

LOSE-ENCODERS-ON-FINISH := false

inline wrap-constructor (f T)
    inline (...)
        bitcast
            f ...
            T

inline define-object (name super release reference)
    type (_ name) <<:: super
        inline __typecall (cls)
            bitcast null cls

        inline __drop (self)
            if ((storagecast self) != null)
                release ('rawptr self)

        inline __rimply (otherT thisT)
            static-if (otherT == super)
                inline (incoming)
                    (dupe incoming) as thisT
            # for handles that descend from this
            elseif ((unqualified otherT) == (superof thisT))
                inline (incoming)
                    bitcast incoming thisT
            elseif (otherT == Nothing)
                inline ()
                    nullof thisT

        inline __imply (thisT otherT)
            static-if ((unqualified otherT) == super)
                inline (self)
                    dupe (self as otherT)

        inline __== (thisT otherT)
            static-if (imply? thisT otherT)
                inline (a b)
                    ('rawptr a) == ('rawptr b)

        inline __hash (self)
            hash ('rawptr (view self))

        inline get-id (self)
            ptrtoint (storagecast (view self)) u64

        inline __copy (self)
            ptr := storagecast (view self)
            if (ptr != null)
                reference ptr

            imply (dupe ptr) (typeof self)

        inline rawptr (self)
            dupe (storagecast self)

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

            t.name = (copy name)

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
    Instance := define-object "WGPUInstance" wgpu.Instance wgpu.InstanceRelease wgpu.InstanceReference
    Adapter := define-object "WGPUAdapter" wgpu.Adapter wgpu.AdapterRelease wgpu.AdapterReference
    BindGroup := define-object "WGPUBindGroup" wgpu.BindGroup wgpu.BindGroupRelease wgpu.BindGroupReference
    BindGroupLayout := define-object "WGPUBindGroupLayout" wgpu.BindGroupLayout wgpu.BindGroupLayoutRelease wgpu.BindGroupLayoutReference
    Buffer := define-object "WGPUBuffer" wgpu.Buffer wgpu.BufferRelease wgpu.BufferReference
    CommandBuffer := define-object "WGPUCommandBuffer" wgpu.CommandBuffer wgpu.CommandBufferRelease wgpu.CommandBufferReference
    CommandEncoder := define-object "WGPUCommandEncoder" wgpu.CommandEncoder wgpu.CommandEncoderRelease wgpu.CommandEncoderReference
    RenderPassEncoder := define-object "WGPURenderPassEncoder" wgpu.RenderPassEncoder wgpu.RenderPassEncoderRelease wgpu.RenderPassEncoderReference
    ComputePassEncoder := define-object "WGPUComputePassEncoder" wgpu.ComputePassEncoder wgpu.ComputePassEncoderRelease wgpu.ComputePassEncoderReference
    RenderBundleEncoder := define-object "WGPURenderBundleEncoder" wgpu.RenderBundleEncoder wgpu.RenderBundleEncoderRelease wgpu.RenderBundleEncoderReference
    ComputePipeline := define-object "WGPUComputePipeline" wgpu.ComputePipeline wgpu.ComputePipelineRelease wgpu.ComputePipelineReference
    Device := define-object "WGPUDevice" wgpu.Device wgpu.DeviceRelease wgpu.DeviceReference
    PipelineLayout := define-object "WGPUPipelineLayout" wgpu.PipelineLayout wgpu.PipelineLayoutRelease wgpu.PipelineLayoutReference
    QuerySet := define-object "WGPUQuerySet" wgpu.QuerySet wgpu.QuerySetRelease wgpu.QuerySetReference
    RenderBundle := define-object "WGPURenderBundle" wgpu.RenderBundle wgpu.RenderBundleRelease wgpu.RenderBundleReference
    RenderPipeline := define-object "WGPURenderPipeline" wgpu.RenderPipeline wgpu.RenderPipelineRelease wgpu.RenderPipelineReference
    Sampler := define-object "WGPUSampler" wgpu.Sampler wgpu.SamplerRelease wgpu.SamplerReference
    ShaderModule := define-object "WGPUShaderModule" wgpu.ShaderModule wgpu.ShaderModuleRelease wgpu.ShaderModuleReference
    Surface := define-object "WGPUSurface" wgpu.Surface wgpu.SurfaceRelease wgpu.SurfaceReference
    Texture := define-object "WGPUTexture" wgpu.Texture wgpu.TextureRelease wgpu.TextureReference
    TextureView := define-object "WGPUTextureView" wgpu.TextureView wgpu.TextureViewRelease wgpu.TextureViewReference

    TextureCreateView := wrap-constructor wgpu.TextureCreateView TextureView
    DeviceCreateTexture := wrap-constructor wgpu.DeviceCreateTexture Texture
    InstanceCreateSurface := wrap-constructor wgpu.InstanceCreateSurface Surface
    DeviceCreateShaderModule := wrap-constructor wgpu.DeviceCreateShaderModule ShaderModule
    DeviceCreateSampler := wrap-constructor wgpu.DeviceCreateSampler Sampler
    DeviceCreateRenderPipeline := wrap-constructor wgpu.DeviceCreateRenderPipeline RenderPipeline
    RenderBundleEncoderFinish := wrap-constructor wgpu.RenderBundleEncoderFinish RenderBundle
    DeviceCreateQuerySet := wrap-constructor wgpu.DeviceCreateQuerySet QuerySet
    DeviceCreatePipelineLayout := wrap-constructor wgpu.DeviceCreatePipelineLayout PipelineLayout
    DeviceCreateComputePipeline := wrap-constructor wgpu.DeviceCreateComputePipeline ComputePipeline
    DeviceCreateRenderBundleEncoder := wrap-constructor wgpu.DeviceCreateRenderBundleEncoder RenderBundleEncoder
    CommandEncoderBeginComputePass := wrap-constructor wgpu.CommandEncoderBeginComputePass ComputePassEncoder
    CommandEncoderBeginRenderPass := wrap-constructor wgpu.CommandEncoderBeginRenderPass RenderPassEncoder
    DeviceCreateCommandEncoder := wrap-constructor wgpu.DeviceCreateCommandEncoder CommandEncoder
    DeviceCreateBuffer := wrap-constructor wgpu.DeviceCreateBuffer Buffer
    DeviceCreateBindGroupLayout := wrap-constructor wgpu.DeviceCreateBindGroupLayout BindGroupLayout
    DeviceCreateBindGroup := wrap-constructor wgpu.DeviceCreateBindGroup BindGroup
    CreateInstance := wrap-constructor wgpu.CreateInstance Instance

    # special cases that transform input
    inline CommandEncoderFinish (cmd-encoder desc)
        result :=
            wgpu.CommandEncoderFinish cmd-encoder desc
        static-if LOSE-ENCODERS-ON-FINISH
            lose cmd-encoder
        imply result CommandBuffer

    type ShaderStageFlags < integer : u32
        let __typecall = (define-flags wgpu.ShaderStage)

    .. (local-scope) wgpu
