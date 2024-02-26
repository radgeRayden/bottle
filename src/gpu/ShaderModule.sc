using import compiler.target.SPIR-V enum String struct
using import .common ..context ..helpers ..logger .types

import .wgpu
from wgpu let typeinit@ chained@

ctx := context-accessor 'gpu

fn shader-module-from-SPIRV (code)
    wgpu.DeviceCreateShaderModule
        ctx.device
        chained@ 'ShaderModuleSPIRVDescriptor
            codeSize = ((countof code) // 4) as u32
            code = (dupe (code as rawstring as (@ u32)))

fn shader-module-from-WGSL (code)
    wgpu.DeviceCreateShaderModule
        ctx.device
        chained@ 'ShaderModuleWGSLDescriptor
            code = (dupe (code as rawstring))

fn shader-module-from-GLSL (code stage)
    local defines =
        arrayof wgpu.ShaderDefine
            typeinit "gl_VertexID" "gl_VertexIndex"
            typeinit "gl_InstanceID" "gl_InstanceIndex"

    wgpu.DeviceCreateShaderModule
        ctx.device
        chained@ 'ShaderModuleGLSLDescriptor
            stage = stage
            code = (dupe (code as rawstring))
            defineCount = (countof defines)
            defines = &defines

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
                static-error "invalid shader source type"

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
            static-error "invalid shader stage"

        vvv bind code
        static-match source-language
        case ShaderLanguage.GLSL
            String
                static-compile-glsl SPV_ENV_OPENGL_4_5 target (static-typify f)
        case ShaderLanguage.SPIRV
            String
                static-compile-spirv SPV_ENV_VULKAN_1_1_SPIRV_1_4 target (static-typify f)
        default
            static-error "invalid shader source type, only SPIRV and GLSL allowed"

        this-function cls code source-language stage
()
