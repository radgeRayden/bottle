using import struct
using import enum
using import String
using import .common
using import ..helpers

let wgpu = (import ..FFI.wgpu)

struct GPUShaderModule
    _handle : wgpu.ShaderModule

    inline shader-module-from-SPIRV (code)
        local desc : wgpu.ShaderModuleSPIRVDescriptor
            chain =
                wgpu.ChainedStruct
                    sType = wgpu.SType.ShaderModuleSPIRVDescriptor
            codeSize = ((countof code) // 4)
            code = (code as rawstring as (@ u32))

        let module =
            wgpu.DeviceCreateShaderModule
                istate.device
                &local wgpu.ShaderModuleDescriptor
                    nextInChain = (&desc as (mutable@ wgpu.ChainedStruct))
        module

    inline shader-module-from-WGSL (code)
        local desc : wgpu.ShaderModuleWGSLDescriptor
            chain =
                wgpu.ChainedStruct
                    sType = wgpu.SType.ShaderModuleWGSLDescriptor
            code = (code as rawstring)

        let module =
            wgpu.DeviceCreateShaderModule
                istate.device
                &local wgpu.ShaderModuleDescriptor
                    nextInChain = (&desc as (mutable@ wgpu.ChainedStruct))
        module

    inline __typecall (cls source flavor stage)
        vvv bind source
        static-match (typeof source)
        case String
            source
        case function
            static-compile-spirv 0x10000 stage (static-typify source)
        default
            static-error "unknown shader source type"

        vvv bind module
        static-match flavor
        case 'spirv
            shader-module-from-SPIRV source
        case 'wgsl
            shader-module-from-WGSL source
        default
            static-error "unknown shader language flavor"

        super-type.__typecall cls
            _handle = module

    inline __drop (self)
        wgpu.ShaderModuleDrop self._handle

    unlet shader-module-from-SPIRV shader-module-from-WGSL

struct GPUPipeline
    _handle : wgpu.RenderPipeline
    _layout : wgpu.PipelineLayout
    _bgroup-layout : wgpu.BindGroupLayout

    fn make-bind-group-layout ()
        local entries =
            arrayof wgpu.BindGroupLayoutEntry
                typeinit
                    binding = 0
                    visibility = wgpu.ShaderStage.Vertex
                    buffer =
                        wgpu.BufferBindingLayout
                            type = wgpu.BufferBindingType.ReadOnlyStorage
                            hasDynamicOffset = false
                            minBindingSize = 0

        let layout =
            wgpu.DeviceCreateBindGroupLayout istate.device
                &local wgpu.BindGroupLayoutDescriptor
                    label = "Bottle bind group layout"
                    entryCount = 1
                    entries = &entries

        assert (layout != null)
        layout

    fn make-pipeline-layout ()
        local bgroup-layout = (make-bind-group-layout)

        let pip-layout =
            wgpu.DeviceCreatePipelineLayout istate.device
                &local wgpu.PipelineLayoutDescriptor
                    bindGroupLayoutCount = 1
                    bindGroupLayouts = &bgroup-layout
        assert (pip-layout != null)

        wgpu.BindGroupLayoutDrop bgroup-layout
        _ pip-layout bgroup-layout

    fn make-pipeline (vertex-module fragment-module)
        # FIXME: gotta clean this up. Overloads?
        let vertex-module fragment-module =
            static-if (none? fragment-module)
                _ vertex-module._handle vertex-module._handle
            else
                _ vertex-module._handle fragment-module._handle

        let pip-layout bgroup-layout = (make-pipeline-layout)
        let pipeline =
            wgpu.DeviceCreateRenderPipeline istate.device
                &local wgpu.RenderPipelineDescriptor
                    label = "bottle render pipeline"
                    layout = pip-layout
                    vertex =
                        wgpu.VertexState
                            module = vertex-module
                            entryPoint = "vs_main"
                    primitive =
                        wgpu.PrimitiveState
                            topology = wgpu.PrimitiveTopology.TriangleList
                            frontFace = wgpu.FrontFace.CCW
                    multisample =
                        wgpu.MultisampleState
                            count = 1
                            mask = (~ 0:u32)
                            alphaToCoverageEnabled = false
                    fragment =
                        &local wgpu.FragmentState
                            module = fragment-module
                            entryPoint = "fs_main"
                            targetCount = 1
                            targets =
                                &local wgpu.ColorTargetState
                                    format = (wgpu.SurfaceGetPreferredFormat istate.surface istate.adapter)
                                    blend =
                                        &local wgpu.BlendState
                                            color =
                                                typeinit
                                                    srcFactor = wgpu.BlendFactor.One
                                                    dstFactor = wgpu.BlendFactor.Zero
                                                    operation = wgpu.BlendOperation.Add
                                            alpha =
                                                typeinit
                                                    srcFactor = wgpu.BlendFactor.One
                                                    dstFactor = wgpu.BlendFactor.Zero
                                                    operation = wgpu.BlendOperation.Add
                                    writeMask = wgpu.ColorWriteMask.All

        wgpu.PipelineLayoutDrop pip-layout
        _ pipeline pip-layout bgroup-layout

    inline __typecall (cls interface vertex-shader fragment-shader)
        let handle layout bgroup-layout = (make-pipeline vertex-shader fragment-shader)
        super-type.__typecall cls
            _handle = handle
            _layout = layout
            _bgroup-layout = bgroup-layout

    fn get-binding-layout (self)
        self._bgroup-layout

    inline __drop (self)
        wgpu.RenderPipelineDrop self._handle

    unlet make-bind-group-layout make-pipeline-layout

do
    let GPUPipeline GPUShaderModule
    locals;
