using import Array enum hash String
import wgpu

# HELPERS
# =======

spice Scope->CEnum (scope tagf)
    vvv bind expr
    fold (expr = `()) for k v in (scope as Scope)
        spice-quote
            expr
            tagf [(k as Symbol)] Nothing v

    spice-quote
        embed
            expr

inline wrap-constructor (f T)
    inline (...)
        bitcast
            f ...
            T

inline define-object (name super release reference)
    typedef (_ name) < pointer :: super
        inline __typecall (cls)
            bitcast null cls

        inline __drop (self)
            if ((storagecast self) != null)
                release ('rawptr self)

        inline __rimply (otherT thisT)
            static-if (otherT == super)
                inline (incoming)
                    bitcast (dupe incoming) thisT
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
                    bitcast ('rawptr self) otherT

        inline __== (thisT otherT)
            static-if (imply? thisT otherT)
                inline (a b)
                    ('rawptr a) == ('rawptr b)

        inline __hash (self)
            hash ('rawptr (view self))

        DumbHandleType := typedef (.. "WGPUDumbHandle<" name ">") : u64 
        type+ DumbHandleType
            inline __== (thisT otherT)
                static-if ((unqualified thisT) == (unqualified otherT))
                    (a b) -> ((storagecast a) == (storagecast b))
                else
                    static-error "can't compare handles of different types"
            inline __hash (self)
                hash (storagecast self)

        inline get-id (self)
            bitcast (dupe (ptrtoint (view self) (storageof DumbHandleType))) DumbHandleType

        inline __copy (self)
            ptr := storagecast (view self)
            if (ptr != null)
                reference ptr

            imply (dupe ptr) (typeof self)

        inline rawptr (self)
            dupe (storagecast (view self))

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

spice split-chained-fields (args...)
    using import slice
    local chained : (Array Value)
    local passthrough : (Array Value)

    for arg in ('args args...)
        k v := 'dekey arg
        kname := k as Symbol as string
        if ((kname @ 0) == (char "."))
            new-name := Symbol (rslice kname 1)
            'append passthrough (sc_keyed_new new-name v)
        else
            'append chained arg

    chained-ptr chained-count := 'data chained
    passthrough-ptr passthrough-count := 'data passthrough
    spice-quote
        _
            inline "chained-fields" ()
                [(sc_argument_list_new (i32 chained-count) (view chained-ptr))]
            inline "passthrough-fields" ()
                [(sc_argument_list_new (i32 passthrough-count) (view passthrough-ptr))]

run-stage;

type+ wgpu.StringView
    inline __rimply (otherT thisT)
        static-if (otherT == String)
            inline (incoming)
                ptr count := 'data incoming
                thisT (dupe ptr) count
        else
            inline (incoming)
                thisT (incoming as rawstring) (countof incoming)

    inline __imply (thisT otherT)
        static-if (otherT == String)
            inline (self)
                String self.data self.length

run-stage;

do
    Instance := define-object "WGPUInstance" wgpu.Instance wgpu.InstanceRelease wgpu.InstanceAddRef
    Adapter := define-object "WGPUAdapter" wgpu.Adapter wgpu.AdapterRelease wgpu.AdapterAddRef
    BindGroup := define-object "WGPUBindGroup" wgpu.BindGroup wgpu.BindGroupRelease wgpu.BindGroupAddRef
    BindGroupLayout := define-object "WGPUBindGroupLayout" wgpu.BindGroupLayout wgpu.BindGroupLayoutRelease wgpu.BindGroupLayoutAddRef
    Buffer := define-object "WGPUBuffer" wgpu.Buffer wgpu.BufferRelease wgpu.BufferAddRef
    CommandBuffer := define-object "WGPUCommandBuffer" wgpu.CommandBuffer wgpu.CommandBufferRelease wgpu.CommandBufferAddRef
    CommandEncoder := define-object "WGPUCommandEncoder" wgpu.CommandEncoder wgpu.CommandEncoderRelease wgpu.CommandEncoderAddRef
    RenderPassEncoder := define-object "WGPURenderPassEncoder" wgpu.RenderPassEncoder wgpu.RenderPassEncoderRelease wgpu.RenderPassEncoderAddRef
    ComputePassEncoder := define-object "WGPUComputePassEncoder" wgpu.ComputePassEncoder wgpu.ComputePassEncoderRelease wgpu.ComputePassEncoderAddRef
    RenderBundleEncoder := define-object "WGPURenderBundleEncoder" wgpu.RenderBundleEncoder wgpu.RenderBundleEncoderRelease wgpu.RenderBundleEncoderAddRef
    ComputePipeline := define-object "WGPUComputePipeline" wgpu.ComputePipeline wgpu.ComputePipelineRelease wgpu.ComputePipelineAddRef
    Device := define-object "WGPUDevice" wgpu.Device wgpu.DeviceRelease wgpu.DeviceAddRef
    PipelineLayout := define-object "WGPUPipelineLayout" wgpu.PipelineLayout wgpu.PipelineLayoutRelease wgpu.PipelineLayoutAddRef
    QuerySet := define-object "WGPUQuerySet" wgpu.QuerySet wgpu.QuerySetRelease wgpu.QuerySetAddRef
    RenderBundle := define-object "WGPURenderBundle" wgpu.RenderBundle wgpu.RenderBundleRelease wgpu.RenderBundleAddRef
    RenderPipeline := define-object "WGPURenderPipeline" wgpu.RenderPipeline wgpu.RenderPipelineRelease wgpu.RenderPipelineAddRef
    Sampler := define-object "WGPUSampler" wgpu.Sampler wgpu.SamplerRelease wgpu.SamplerAddRef
    ShaderModule := define-object "WGPUShaderModule" wgpu.ShaderModule wgpu.ShaderModuleRelease wgpu.ShaderModuleAddRef
    Surface := define-object "WGPUSurface" wgpu.Surface wgpu.SurfaceRelease wgpu.SurfaceAddRef
    Texture := define-object "WGPUTexture" wgpu.Texture wgpu.TextureRelease wgpu.TextureAddRef
    TextureView := define-object "WGPUTextureView" wgpu.TextureView wgpu.TextureViewRelease wgpu.TextureViewAddRef

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
        imply result CommandBuffer

    type ShaderStageFlags < integer : u64
        let __typecall = (define-flags wgpu.ShaderStage)

    inline typeinit@ (...)
        implies (T)
            static-assert (T < pointer)
            imply (& (local hidden = (elementof T) ...)) T

    inline make-chained@ (T)
        inline (K ...)
            using wgpu
            chaintypename := K
            K := getattr wgpu K
            chaintype := static-try (getattr SType chaintypename)
            else
                (getattr NativeSType chaintypename) as (storageof SType) as SType

            chained-fields passthrough-fields := split-chained-fields ...

            typeinit@
                nextInChain = as
                    &
                        local := K
                            chain = typeinit
                                sType = chaintype
                            (chained-fields)
                    mutable@ T
                (passthrough-fields)
    chained@ := make-chained@ wgpu.ChainedStruct
    chained-out@ := make-chained@ wgpu.ChainedStructOut
    unlet make-chained@

    inline reconstruct-enum (name scope)
        enum (_ name) : u64
            Scope->CEnum scope tag

    InstanceBackend := reconstruct-enum "InstanceBackend" wgpu.InstanceBackend
    InstanceFlag := reconstruct-enum "InstanceFlag" wgpu.InstanceFlag
    BufferUsage := reconstruct-enum "BufferUsage" wgpu.BufferUsage
    ColorWriteMask := reconstruct-enum "ColorWriteMask" wgpu.ColorWriteMask
    MapMode := reconstruct-enum "MapMode" wgpu.MapMode
    ShaderStage := reconstruct-enum "ShaderStage" wgpu.ShaderStage
    TextureUsage := reconstruct-enum "TextureUsage" wgpu.TextureUsage

    .. (local-scope) wgpu
