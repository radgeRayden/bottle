using import Array glm property String struct

using import .common ..context ..helpers ..asset.ImageData ..exceptions \
    .texture-format .types radl.ext radl.strfmt

import .module .wgpu
from wgpu let typeinit@ chained@

ctx := context-accessor 'gpu

fn max-mipmap-count (width height depth)
    m := max (max width height) depth
    u32 ((floor (log2 (f32 m))) + 1)

fn mip-level-size (dimension size level)
    scale := (d) -> (max 1:u32 (d >> (u32 level)))
    x y z := unpack size

    uvec3
        switch (imply dimension wgpu.TextureDimension)
        case '1D
            _ (scale x) 1:u32 1:u32
        case '2D
            _ (scale x) (scale y) z
        case '3D
            _ (scale x) (scale y) (scale z)
        default (abort)

struct MipmapUniformData
    mip-level-size : (array vec4 32)

fn mipmap-vertex-shader ()
    using import glsl
    using import glm

    local texcoords =
        arrayof vec2
            vec2 (0.0, 0.0)
            vec2 (0.0, 1.0)
            vec2 (1.0, 1.0)
            vec2 (1.0, 1.0)
            vec2 (1.0, 0.0)
            vec2 (0.0, 0.0)

    out vtexcoords : vec2 (location = 0)
    uniform uniforms : MipmapUniformData
        set = 0
        binding = 2

    render-target-size := vec2 4096
    mipmap-size := uniforms.mip-level-size @ gl_InstanceIndex
    extent := (mipmap-size.xy / render-target-size) * 2.0

    w h := extent.x, extent.y
    local vertices =
        arrayof vec3
            vec3 (    -1.0,     1.0, 0.0) #tl
            vec3 (    -1.0, 1.0 - h, 0.0) #bl
            vec3 (-1.0 + w, 1.0 - h, 0.0) #br
            vec3 (-1.0 + w, 1.0 - h, 0.0) #br
            vec3 (-1.0 + w,     1.0, 0.0) #tr
            vec3 (    -1.0,     1.0, 0.0) #tl

    idx := gl_VertexIndex
    vtexcoords = texcoords @ idx
    gl_Position = (vec4 (vertices @ idx) 1.0)

fn mipmap-fragment-shader ()
    using import glsl
    using import glm

    uniform s : sampler (set = 0) (binding = 0)
    uniform t : texture2D (set = 0) (binding = 1)

    in vtexcoords : vec2 (location = 0)
    out fcolor : vec4 (location = 0)

    fcolor = texture (sampler2D t s) vtexcoords
    ()

fn mipmap-pipeline-layout ()
    module.get-internal-pipeline-layout S"mipmap-gen-pipeline-layout"
        fn ()
            local bg-entries =
                arrayof wgpu.BindGroupLayoutEntry
                    typeinit
                        binding = 0
                        visibility = wgpu.ShaderStage.Fragment
                        sampler =
                            typeinit
                                type = 'Filtering
                    typeinit
                        binding = 1
                        visibility = wgpu.ShaderStage.Fragment
                        texture =
                            typeinit
                                sampleType = 'Float
                                viewDimension = '2D
                                multisampled = false
                    typeinit
                        binding = 2
                        visibility = wgpu.ShaderStage.Vertex
                        buffer =
                            typeinit
                                type = 'Uniform

            local bg-layout =
                do
                    wgpu.DeviceCreateBindGroupLayout ctx.device
                        typeinit@
                            label = "bottle mipmap bindgroup layout"
                            entryCount = (countof bg-entries)
                            entries = &bg-entries

            local bg-layouts =
                arrayof (storageof wgpu.BindGroupLayout)
                    'rawptr bg-layout

            pip-layout :=
                wgpu.DeviceCreatePipelineLayout ctx.device
                    typeinit@
                        label = "bottle mipmap gen pip layout"
                        bindGroupLayoutCount = (countof bg-layouts)
                        bindGroupLayouts = &bg-layouts

            imply pip-layout PipelineLayout

fn mipmap-pipeline (color-format)
    module.get-internal-pipeline f"mipmap-gen-pipeline-${color-format}"
        fn (color-format)
            pip-layout := (mipmap-pipeline-layout)
            vert := ShaderModule mipmap-vertex-shader 'SPIRV 'Vertex
            frag := ShaderModule mipmap-fragment-shader 'SPIRV 'Fragment

            wgpu.DeviceCreateRenderPipeline ctx.device
                typeinit@
                    label = f"mipmap-gen-pipeline-${color-format}"
                    layout = pip-layout
                    vertex =
                        typeinit
                            module = vert
                            entryPoint = "main"
                    primitive =
                        typeinit
                            topology = 'TriangleList
                            frontFace = 'CCW
                            cullMode = 'None
                    fragment =
                        typeinit@
                            module = frag
                            entryPoint = "main"
                            targetCount = 1
                            targets =
                                typeinit@
                                    format = color-format
                                    writeMask = wgpu.ColorWriteMask.All
                                    blend =
                                        typeinit@
                                            color =
                                                wgpu.BlendComponent
                                                    operation = 'Add
                                                    srcFactor = 'SrcAlpha
                                                    dstFactor = 'OneMinusSrcAlpha
                                            alpha =
                                                wgpu.BlendComponent
                                                    operation = 'Add
                                                    srcFactor = 'One
                                                    dstFactor = 'OneMinusSrcAlpha
                    multisample =
                        typeinit
                            count = 1
                            mask = ~0:u32
        color-format

fn mipmap-render-target (color-format)
    # TODO: estimate a texture size and grow if necessary
    module.get-internal-texture f"mipmap-gen-render-target-${color-format}"
        fn (color-format)
            wgpu.DeviceCreateTexture ctx.device
                typeinit@
                    label = "bottle mipmap RT"
                    usage = (|
                                wgpu.TextureUsage.CopySrc
                                wgpu.TextureUsage.TextureBinding
                                wgpu.TextureUsage.RenderAttachment)
                    dimension = '2D
                    size = typeinit 4096:u32 4096:u32 1:u32
                    format = color-format
                    mipLevelCount = 1
                    sampleCount = 1
        color-format

fn mipmap-sampler (level)
    module.get-internal-sampler f"mipmap-gen-sampler-${level}"
        fn (level)
            wgpu.DeviceCreateSampler ctx.device
                typeinit@
                    addressModeU = 'ClampToEdge
                    addressModeV = 'ClampToEdge
                    addressModeW = 'ClampToEdge
                    magFilter = 'Linear
                    minFilter = 'Linear
                    lodMinClamp = (f32 level) - 1.0
                    lodMaxClamp = (f32 level) - 1.0
                    mipmapFilter = 'Nearest
                    maxAnisotropy = 1
        level

type+ Texture
    inline... __typecall (cls,
                          width : u32,
                          height : u32,
                          slices : u32 = 1:u32,
                          format : (param? wgpu.TextureFormat) = none,
                          image-data : (param? ImageData) = none,
                          render-target? : bool = false,
                          dimension : wgpu.TextureDimension = '2D,
                          mipmap-levels : u32 = 1:u32,
                          sample-count : u32 = 1:u32)

        usage :=
            |   wgpu.TextureUsage.CopySrc
                wgpu.TextureUsage.CopyDst
                wgpu.TextureUsage.TextureBinding
                (render-target? wgpu.TextureUsage.RenderAttachment (bitcast 0:u64 wgpu.TextureUsage))

        let format =
            static-if (none? format)
                image-data.format
            else format

        handle :=
            wgpu.DeviceCreateTexture ctx.device
                &local wgpu.TextureDescriptor
                    label = f"Bottle Texture ${format}"
                    usage = usage
                    dimension = dimension
                    size = (wgpu.Extent3D width height slices)
                    format = format
                    mipLevelCount = (mipmap-levels == 0) (max-mipmap-count width height slices) mipmap-levels
                    sampleCount = sample-count

        self := wrap-nullable-object cls handle

        static-if (not (none? image-data))
            'frame-write (view self) image-data

        self
    case (cls, image-data : ImageData)
        this-function cls
            copy image-data.width
            copy image-data.height
            copy image-data.slices
            copy image-data.format
            image-data
    case (cls)
        bitcast null this-type

    fn... frame-write (self, image-data : ImageData, x : u32 = 0:u32, y : u32 = 0:u32, z : u32 = 0:u32, mip-level : u32 = 0:u32, aspect : wgpu.TextureAspect = 'All)
        format := self.Format
        if (image-data.format != format)
            raise GPUError.InvalidOperation #S"Mismatched formats between ImageData and Texture"

        block-size := get-texel-block-size format aspect

        iwidth iheight islices := image-data.width, image-data.height, image-data.slices
        twidth theight tslices := unpack self.Size
        data-size          := iwidth * iheight * islices * block-size
        texture-size-bytes := twidth * theight * tslices * block-size

        ptr count := 'data image-data.data
        assert (count == data-size) "malformed ImageData"

        write-offset := ((twidth * theight * z) + (twidth * y) + x) * block-size
        assert (data-size <= (texture-size-bytes - write-offset))

        wgpu.QueueWriteTexture ctx.queue
            &local wgpu.TexelCopyTextureInfo
                texture = self
                mipLevel = mip-level
                origin = (wgpu.Origin3D x y z)
                aspect = aspect
            ptr
            data-size
            &local wgpu.TexelCopyBufferLayout
                offset = 0 #buffer-offset
                bytesPerRow = iwidth * block-size
                rowsPerImage = iheight
            &local wgpu.Extent3D iwidth iheight islices

    fn... generate-mipmaps (self, downsample-filter : wgpu.FilterMode = wgpu.FilterMode.Linear)
        using import .types

        levels := imply self.MipLevelCount u32
        if (levels == 1:u32)
            return;

        color-format := self.Format
        pipeline := (mipmap-pipeline color-format)
        bg-layout := 'get-bind-group-layout pipeline 0
        render-target := (mipmap-render-target color-format)
        cmd-encoder := ctx.cmd-encoder
        texture-view :=
            TextureView self
                mip-level-count = 1

        rt-view :=
            TextureView (view render-target)
                mip-level-count = 1

        uniforms := ((UniformBuffer MipmapUniformData) 1)
        local uniform-data : MipmapUniformData
        for i in (range levels)
            x y z := unpack (mip-level-size self.Dimension self.Size i)
            (uniform-data.mip-level-size @ i) = vec4 x y z 0
            'frame-write uniforms (view uniform-data)

        # NOTE: if the texture is already a render target, then copying can be ellided.
        # Naturally we don't need to render to the first mip level
        for i in (range 1:u32 levels)
            local bind-group = 'builder BindGroup
            'set-layout bind-group bg-layout
            'add-entry bind-group (mipmap-sampler i)
            'add-entry bind-group texture-view
            'add-entry bind-group uniforms

            bind-group := 'finalize bind-group

            render-pass :=
                wgpu.CommandEncoderBeginRenderPass cmd-encoder
                    typeinit@
                        label = f"bottle mipmap renderpass ${i}"
                        colorAttachmentCount = 1
                        colorAttachments =
                            typeinit@
                                view = rt-view
                                loadOp = 'Clear
                                storeOp = 'Store
                                clearValue = typeinit 1.0 1.0 1.0 1.0

            wgpu.RenderPassEncoderSetPipeline render-pass pipeline
            wgpu.RenderPassEncoderSetBindGroup render-pass 0 bind-group 0 null
            wgpu.RenderPassEncoderDraw render-pass 6 1 0 i
            wgpu.RenderPassEncoderEnd render-pass

            'copy-texture cmd-encoder \
                render-target 0 (uvec3) 'All \
                self i (uvec3) 'All \
                (mip-level-size self.Dimension self.Size i)

    let Size =
        property
            inline (self)
                width := wgpu.TextureGetWidth self
                height := wgpu.TextureGetHeight self
                slices := wgpu.TextureGetDepthOrArrayLayers self
                uvec3 width height slices

    let Dimension =
        property
            (self) -> (wgpu.TextureGetDimension self)

    let Format =
        property
            (self) -> (wgpu.TextureGetFormat self)

    let MipLevelCount =
        property
            (self) -> (wgpu.TextureGetMipLevelCount self)

    let SampleCount =
        property
            (self) -> (wgpu.TextureGetSampleCount self)

    let MaxTextureSize1D =
        spice-quote
            deref ctx.limits.maxTextureDimension1D

    let MaxTextureSize2D =
        spice-quote
            deref ctx.limits.maxTextureDimension2D

    let MaxTextureSize3D =
        spice-quote
            deref ctx.limits.maxTextureDimension3D

    let MaxTextureArrayLayers =
        spice-quote
            deref ctx.limits.maxTextureArrayLayers

type+ TextureView
    inline... __typecall (cls, source-texture : Texture)
        wrap-nullable-object cls
            wgpu.TextureCreateView source-texture null
    case (cls, source-texture : Texture,
            dimension : wgpu.TextureViewDimension = '2D,
            base-mip : u32 = 0:u32,
            mip-level-count : (param? u32) = none,
            base-array-layer : u32 = 0:u32,
            array-layer-count : u32 = 1:u32,
            aspect : wgpu.TextureAspect = 'All,
            format : (param? wgpu.TextureFormat) = none)

        let mip-level-count =
            static-if (none? mip-level-count)
                source-texture.MipLevelCount
            else
                mip-level-count

        let view-format =
            static-if (none? format)
                source-texture.Format
            else
                format

        wrap-nullable-object cls
            wgpu.TextureCreateView source-texture
                typeinit@
                    label = "bottle texture view"
                    format = view-format
                    dimension = dimension
                    baseMipLevel = base-mip
                    mipLevelCount = mip-level-count
                    baseArrayLayer = base-array-layer
                    arrayLayerCount = array-layer-count
                    aspect = aspect
    case (cls)
        bitcast null cls

do
    let TextureView Texture
    local-scope;
