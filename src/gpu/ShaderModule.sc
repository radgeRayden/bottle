using import compiler.target.SPIR-V
using import enum
using import String
using import struct

using import .common
using import ..helpers
using import .types
import .wgpu

fn shader-module-from-SPIRV (code)
    local desc : wgpu.ShaderModuleSPIRVDescriptor
        chain =
            wgpu.ChainedStruct
                sType = wgpu.SType.ShaderModuleSPIRVDescriptor
        codeSize = ((countof code) // 4) as u32
        code = (dupe (code as rawstring as (@ u32)))

    let module =
        wgpu.DeviceCreateShaderModule
            istate.device
            &local wgpu.ShaderModuleDescriptor
                nextInChain = (&desc as (mutable@ wgpu.ChainedStruct))
    module

fn shader-module-from-WGSL (code)
    local desc : wgpu.ShaderModuleWGSLDescriptor
        chain =
            wgpu.ChainedStruct
                sType = wgpu.SType.ShaderModuleWGSLDescriptor
        code = (dupe (code as rawstring))

    let module =
        wgpu.DeviceCreateShaderModule
            istate.device
            &local wgpu.ShaderModuleDescriptor
                nextInChain = (&desc as (mutable@ wgpu.ChainedStruct))
    module

fn shader-module-from-GLSL (code stage)
    local defines =
        arrayof wgpu.ShaderDefine
            typeinit "gl_VertexID" "gl_VertexIndex"
            typeinit "gl_InstanceID" "gl_InstanceIndex"

    local desc : wgpu.ShaderModuleGLSLDescriptor
        chain =
            wgpu.ChainedStruct
                sType = bitcast wgpu.NativeSType.ShaderModuleGLSLDescriptor wgpu.SType
        stage = stage
        code = (dupe (code as rawstring))
        defineCount = (countof defines)
        defines = &defines

    let module =
        wgpu.DeviceCreateShaderModule
            istate.device
            &local wgpu.ShaderModuleDescriptor
                nextInChain = (&desc as (mutable@ wgpu.ChainedStruct))
    module

type+ ShaderModule
    inline... __typecall (cls, source : String, source-language : ShaderLanguage, ...)
        stage := ...
        let module =
            static-match source-language
            case ShaderLanguage.WGSL
                shader-module-from-WGSL source
            case ShaderLanguage.GLSL
                static-if (not (none? stage))
                    shader-module-from-GLSL source stage
                else
                    static-error "glsl shaders require stage information"
            case ShaderLanguage.SPIRV
                shader-module-from-SPIRV source
            default
                assert false "invalid shader source type"

        wrap-nullable-object cls module

    case (cls, f : Closure, source-language : ShaderLanguage, ...)
        stage := ...
        static-if (none? stage)
            static-error "scopes shaders require stage information"

        vvv bind target
        static-match stage
        case wgpu.ShaderStage.Vertex
            'vertex
        case wgpu.ShaderStage.Fragment
            'fragment
        case wgpu.ShaderStage.Compute
            'compute
        default
            assert false "invalid shader stage"

        vvv bind code
        static-match source-language
        case ShaderLanguage.GLSL
            String
                static-compile-glsl SPV_ENV_OPENGL_4_5 target (static-typify f)
        case ShaderLanguage.SPIRV
            String
                static-compile-spirv SPV_ENV_VULKAN_1_1_SPIRV_1_4 target (static-typify f)
        default
            assert false "invalid shader source type, only SPIRV and GLSL allowed"

        this-function cls code source-language stage
()
