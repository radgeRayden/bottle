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

    fn make-pipeline (layout-name vertex-module fragment-module)
        # FIXME: gotta clean this up. Overloads?
        let vertex-module fragment-module =
            static-if (none? fragment-module)
                _ vertex-module._handle vertex-module._handle
            else
                _ vertex-module._handle fragment-module._handle

        let pip-layout =
            try
                'get istate.cached-layouts.pipeline-layouts layout-name
            else
                assert false ((String "unknown pipeline layout: ") .. layout-name)
                unreachable;

        let pipeline =
            wgpu.DeviceCreateRenderPipeline istate.device
                &local wgpu.RenderPipelineDescriptor
                    label = "Bottle Render Pipeline"
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
        _ pipeline

    inline __typecall (cls interface-name vertex-shader fragment-shader)
        super-type.__typecall cls
            _handle = (make-pipeline interface-name vertex-shader fragment-shader)

    inline __drop (self)
        wgpu.RenderPipelineDrop self._handle

do
    let GPUPipeline GPUShaderModule
    locals;
