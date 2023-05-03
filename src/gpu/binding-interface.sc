using import enum
using import struct
using import Array
using import String

using import ..helpers

import wgpu

sugar define-interface (name elements...)
    inline define-interface (name elements...)
        typedef (tostring name) : (storageof Nothing)
            let entries =
                arrayof type
                    elements...

            inline __typecall (cls)
                bitcast none cls

    let map... =
        fold (result = '()) for i elem in (enumerate (elements... as list))
            cons
                qq
                    'define-symbol [name] (sugar-quote [elem]) [i]
                result

    let result =
        qq
            [let] [name] =
                do
                    [let] [name] =
                        [define-interface] (sugar-quote [name])
                            unquote-splice elements...
                    unquote-splice map...
                    [name]
    result

;
run-stage;

enum GPUResourceBinding
    Invalid
    Buffer :
        buffer = wgpu.Buffer
        offset = u64
        size = u64
    UniformBuffer :
        buffer = wgpu.Buffer
        offset = u64
        size = u64
    Sampler : wgpu.Sampler
    TextureView : wgpu.TextureView

    fn make-wgpu-descriptor (self)
        dispatch self
        case Buffer (buffer offset size)
            wgpu.BindGroupEntry
                buffer = buffer
                offset = offset
                size = size
        case UniformBuffer (buffer offset size)
            wgpu.BindGroupEntry
                buffer = buffer
                offset = offset
                size = size
        case Sampler (handle)
            wgpu.BindGroupEntry
                sampler = handle
        case TextureView (handle)
            wgpu.BindGroupEntry
                textureView = handle
        default
            unreachable;

    inline __hash (self)
        dispatch self
        case Buffer (handle offset size)
            hash handle offset size
        case Sampler (handle)
            hash handle
        case TextureView (handle)
            hash handle
        default
            unreachable;

    inline __typecall (cls)
        this-type.Invalid;

# so we can "use" the enum later without duplicating the type
run-stage;

# DEFAULT INTERFACES
# ================================================================================
using GPUResourceBinding
vvv bind sets
do
    define-interface StreamingMesh
        Buffer
        Sampler
        TextureView

    define-interface Uniforms
        UniformBuffer

    locals;
run-stage;

using sets
enum GPUBindingLayout
    Empty :
        define-interface Empty
    Basic :
        define-interface Basic
            StreamingMesh
            Uniforms

    inline __getattr (self attr)
        'apply self
            inline (T self)
                getattr (elementof T.Type 0) attr

# DEFAULT RESOURCE GENERATION
# ================================================================================

# To be used when no resource is specified for a certain slot in a bind group
fn make-dummy-resources (istate)
    let dummies = istate.dummy-resources

    let buffer-size = ((sizeof f32) * 4)
    local buf =
        wgpu.DeviceCreateBuffer istate.device
            &local wgpu.BufferDescriptor
                label = "Dummy Bottle Buffer"
                usage = wgpu.BufferUsage.Storage
                size = buffer-size
                mappedAtCreation = true
    let bufdata = (wgpu.BufferGetMappedRange buf 0 buffer-size)
    for i in (range 4)
        (bufdata as (mutable@ f32)) @ i = 1.0
    wgpu.BufferUnmap buf

    dummies.buffer =
        GPUResourceBinding.Buffer
            buffer = buf
            offset = 0
            size = buffer-size

    local buf =
        wgpu.DeviceCreateBuffer istate.device
            &local wgpu.BufferDescriptor
                label = "Dummy Bottle Buffer"
                usage = wgpu.BufferUsage.Uniform
                size = buffer-size
                mappedAtCreation = true
    let bufdata = (wgpu.BufferGetMappedRange buf 0 buffer-size)
    for i in (range 4)
        (bufdata as (mutable@ f32)) @ i = 1.0
    wgpu.BufferUnmap buf

    dummies.uniform-buffer =
        GPUResourceBinding.UniformBuffer
            buffer = buf
            offset = 0
            size = buffer-size

    dummies.sampler =
        GPUResourceBinding.Sampler
            wgpu.DeviceCreateSampler istate.device
                &local wgpu.SamplerDescriptor
                    label = "Dummy Bottle Sampler"
                    addressModeU = wgpu.AddressMode.Repeat
                    addressModeV = wgpu.AddressMode.Repeat
                    addressModeW = wgpu.AddressMode.Repeat
                    magFilter = wgpu.FilterMode.Linear
                    minFilter = wgpu.FilterMode.Linear
                    mipmapFilter = wgpu.MipmapFilterMode.Linear
                    # might need to configure extra stuff

    # let this leak?
    local dummy-texture =
        wgpu.DeviceCreateTexture istate.device
            &local wgpu.TextureDescriptor
                label = "Dummy Bottle Texture"
                usage = (wgpu.TextureUsage.CopyDst | wgpu.TextureUsage.TextureBinding)
                dimension = wgpu.TextureDimension.2D
                size = (wgpu.Extent3D 1 1 1)
                format = wgpu.TextureFormat.RGBA8UnormSrgb
                mipLevelCount = 1
                sampleCount = 1

    local data = (arrayof u8 255 255 255 255)
    let width height = 1:u32 1:u32
    wgpu.QueueWriteTexture istate.queue
        &local wgpu.ImageCopyTexture
            texture = dummy-texture
            mipLevel = 0
            origin = (wgpu.Origin3D)
            aspect = wgpu.TextureAspect.All
        &data as voidstar
        width * height * 4
        &local wgpu.TextureDataLayout
            offset = 0
            bytesPerRow = (width * (sizeof f32))
            rowsPerImage = (height as u32)
        &local wgpu.Extent3D (width as u32) (height as u32) 1

    dummies.texture-view =
        GPUResourceBinding.TextureView
            wgpu.TextureCreateView dummy-texture
                &local wgpu.TextureViewDescriptor
                    format = wgpu.TextureFormat.RGBA8UnormSrgb
                    dimension = wgpu.TextureViewDimension.2D
                    baseMipLevel = 0
                    mipLevelCount = 1
                    baseArrayLayer = 0
                    arrayLayerCount = 1
                    aspect = wgpu.TextureAspect.All
    ;

inline bind-group-layout-from-interface (istate name interface)
    inline make-entry (i ...)
        wgpu.BindGroupLayoutEntry
            binding = (i as u32)
            visibility = (wgpu.ShaderStage.Vertex | wgpu.ShaderStage.Fragment | wgpu.ShaderStage.Compute)
            ...

    local bgroup-entries : (Array wgpu.BindGroupLayoutEntry)

    va-map
        inline (i)
            entry := (interface.entries @ i)

            'append bgroup-entries
                match entry
                case GPUResourceBinding.Buffer
                    make-entry i
                        buffer =
                            wgpu.BufferBindingLayout
                                type = wgpu.BufferBindingType.ReadOnlyStorage
                                minBindingSize = 0
                case GPUResourceBinding.UniformBuffer
                    make-entry i
                        buffer =
                            wgpu.BufferBindingLayout
                                type = wgpu.BufferBindingType.Uniform
                                minBindingSize = 0
                case GPUResourceBinding.Sampler
                    make-entry i
                        sampler =
                            wgpu.SamplerBindingLayout
                                type = wgpu.SamplerBindingType.Filtering
                case GPUResourceBinding.TextureView
                    make-entry i
                        texture =
                            wgpu.TextureBindingLayout
                                sampleType = wgpu.TextureSampleType.Float
                                viewDimension = wgpu.TextureViewDimension.2D
                default
                    assert false

        va-range ((countof interface.entries) as i32)

    wgpu.DeviceCreateBindGroupLayout istate.device
        &local wgpu.BindGroupLayoutDescriptor
            label = (.. (String name) " Bottle Bind Group Layout")
            entryCount = ((countof interface.entries) as u32)
            entries = (imply bgroup-entries pointer)

spice gen-bgroup-layouts (istate cache)
    let expr = (sc_expression_new)
    for k v in sets
        let name = (k as Symbol as string)
        sc_expression_append expr
            spice-quote
                'set cache (String [name])
                    bind-group-layout-from-interface istate [name] v
    expr
run-stage;

fn make-default-pipeline-layouts (istate)
    let bgroup-layout-cache = istate.cached-layouts.bind-group-layouts
    let pip-layout-cache = istate.cached-layouts.pipeline-layouts

    # Populate Bind Group Layout cache
    gen-bgroup-layouts istate bgroup-layout-cache

    # Generate Pipeline Layouts
    va-map
        inline (f)
            let Name Type = (tostring f.Name) (elementof f.Type 0)

            local bgroup-layouts : (Array wgpu.BindGroupLayout)
            let bgroup-count = (countof Type.entries)
            va-map
                inline (i)
                    let T = (Type.entries @ i)
                    'append bgroup-layouts
                        try
                            'get bgroup-layout-cache (String (tostring T))
                        except (ex)
                            assert false (tostring T)
                    ;
                va-range (bgroup-count as i32)

            let pip-layout =
                wgpu.DeviceCreatePipelineLayout istate.device
                    &local wgpu.PipelineLayoutDescriptor
                        label = (.. (String Name) " Bottle Pipeline Layout")
                        bindGroupLayoutCount = (bgroup-count as u32)
                        bindGroupLayouts = (imply bgroup-layouts pointer)

            'set pip-layout-cache (String Name) pip-layout

        GPUBindingLayout.__fields__
    ;

do
    let
        make-dummy-resources
        make-default-pipeline-layouts
        GPUResourceBinding
    locals;
