using import enum
using import struct
using import Array

using import ..helpers

let wgpu = (import ..FFI.wgpu)

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
    Buffer :
        buffer = wgpu.Buffer
        offset = u64
        size = u64
    Sampler : wgpu.Sampler
    TextureView : wgpu.TextureView

    fn make-wgpu-descriptor (self)
        dispatch self
        case Buffer (handle offset size)
            wgpu.BindGroupEntry
                buffer = handle
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
        Buffer

    locals;
run-stage;

using sets
enum GPUBindingLayout
    Basic :
        define-interface Basic
            StreamingMesh
            Uniforms

    inline __getattr (self attr)
        'apply self
            inline (T self)
                getattr (elementof T.Type 0) attr

fn make-dummy-resources (istate)
    let dummies = istate.dummy-resources

    let buffer-size = ((sizeof f32) * 4)
    dummies.buffer =
        wgpu.DeviceCreateBuffer istate.device
            &local wgpu.BufferDescriptor
                label = "Dummy Bottle Buffer"
                usage = wgpu.BufferUsage.Storage
                size = buffer-size
                mappedAtCreation = true

    let bufdata = (wgpu.BufferGetMappedRange dummies.buffer 0 buffer-size)
    for i in (range 4)
        (bufdata as (mutable@ f32)) @ i = 1.0
    wgpu.BufferUnmap dummies.buffer

    dummies.sampler =
        wgpu.DeviceCreateSampler istate.device
            &local wgpu.SamplerDescriptor
                label = "Dummy Bottle Sampler"
                addressModeU = wgpu.AddressMode.Repeat
                addressModeV = wgpu.AddressMode.Repeat
                addressModeW = wgpu.AddressMode.Repeat
                magFilter = wgpu.FilterMode.Linear
                minFilter = wgpu.FilterMode.Linear
                mipmapFilter = wgpu.FilterMode.Linear
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

fn make-default-pipeline-layouts (istate)
    # let bgroup-layouts =
    #     arrayof wgpu.BindGroupLayout
    #         va-map
    # let pipeline-layouts =
    #     arrayof wgpu.PipelineLayout
    #         va-map

do
    let
        make-dummy-resources
        make-default-pipeline-layouts
    locals;
