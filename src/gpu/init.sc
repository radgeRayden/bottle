using import struct
using import ..helpers
using import .istate
using import .errors
using import .render-pass

import ..window

let sdl = (import ..FFI.sdl)
let wgpu = (import ..FFI.wgpu)

# MODULE FUNCTIONS START HERE
# ================================================================================
fn create-surface ()
    static-match operating-system
    case 'linux
        let x11-display x11-window = (window.get-native-info)
        wgpu.InstanceCreateSurface null
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromXlib
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromXlib
                            display = (x11-display as voidstar)
                            window = (x11-window as u32)
                        mutable@ wgpu.ChainedStruct
    case 'windows
        let hinstance hwnd = (window.get-native-info)
        wgpu.InstanceCreateSurface null
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromWindowsHWND
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromWindowsHWND
                            hinstance = hinstance
                            hwnd = hwnd
                        mutable@ wgpu.ChainedStruct
    default
        error "OS not supported"

fn create-swapchain (width height)
    wgpu.DeviceCreateSwapChain istate.device istate.surface
        &local wgpu.SwapChainDescriptor
            label = "swapchain"
            usage = wgpu.TextureUsage.RenderAttachment
            format = (wgpu.SurfaceGetPreferredFormat istate.surface istate.adapter)
            width = (width as u32)
            height = (height as u32)
            presentMode = wgpu.PresentMode.Fifo

fn vshader ()
    using import glsl
    using import glm
    struct VertexAttributes plain
        position : vec3
        color : vec4

    buffer attrs :
        struct AttributeArray plain
            data : (array VertexAttributes)
        set = 0
        binding = 0

    out vcolor : vec4
        location = 0

    let vertex = (attrs.data @ gl_VertexIndex)

    gl_Position = (vec4 vertex.position 1)
    vcolor = vertex.color

let vshader-WGSL =
    """"struct VertexAttributes {
            position : vec3<f32>;
            color : vec4<f32>;
        };

        struct Attributes {
            data : [[stride(32)]] array<VertexAttributes>;
        };

        [[group(0), binding(0)]] var<storage, read> attrs : Attributes;

        struct VertexOutput {
            [[location(0)]] vcolor: vec4<f32>;
            [[builtin(position)]] position: vec4<f32>;
        };

        [[stage(vertex)]]
        fn vs_main([[builtin(vertex_index)]] vindex: u32) -> VertexOutput {
            var out: VertexOutput;
            var vertex = attrs.data[vindex];
            out.vcolor = vertex.color;
            out.position = vec4<f32>(vertex.position, 1.0);
            return out;
        }

fn fshader ()
    using import glsl
    using import glm

    in vcolor : vec4
        location = 0
    out fcolor : vec4
        location = 0

    fcolor = vcolor

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

fn make-default-bgroup-layout ()
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

    wgpu.DeviceCreateBindGroupLayout istate.device
        &local wgpu.BindGroupLayoutDescriptor
            label = "bottle bind group layout"
            entryCount = 1
            entries = &entries

fn make-default-pipeline ()
    let vertex-module fragment-module =
        shader-module-from-WGSL
            vshader-WGSL
        # shader-module-from-SPIRV
        #     static-compile-spirv 0x10000 'vertex (static-typify vshader) 'dump-disassembly
        shader-module-from-SPIRV
            static-compile-spirv 0x10000 'fragment (static-typify fshader)

    let pip-layout =
        wgpu.DeviceCreatePipelineLayout istate.device
            &local wgpu.PipelineLayoutDescriptor
                bindGroupLayoutCount = 1
                bindGroupLayouts = &istate.default-bgroup-layout

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
                    entryPoint = "main"
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

fn bind-buffer (render-pass buffer)
    wgpu.RenderPassEncoderSetBindGroup render-pass._handle 0
        buffer.bgroup
        0
        null

fn init ()
    istate.surface = (create-surface)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter null
        &local wgpu.RequestAdapterOptions
            compatibleSurface = istate.surface
            powerPreference = wgpu.PowerPreference.HighPerformance
        fn (status result msg userdata)
            istate.adapter = result
            ;
        null

    wgpu.AdapterRequestDevice istate.adapter
        &local wgpu.DeviceDescriptor
            requiredLimits =
                &local wgpu.RequiredLimits
        fn (status result msg userdata)
            istate.device = result
            ;
        null

    wgpu.DeviceSetUncapturedErrorCallback istate.device
        fn (errtype message userdata)
            print
                errtype as wgpu.ErrorType
                string message
        null

    istate.swapchain = (create-swapchain (window.get-size))
    istate.queue = (wgpu.DeviceGetQueue istate.device)

    istate.default-bgroup-layout = (make-default-bgroup-layout)
    assert (istate.default-bgroup-layout != null)
    istate.default-pipeline = (make-default-pipeline)
    assert (istate.default-pipeline != null)
    ;

fn begin-frame ()
    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        istate.swapchain = (create-swapchain (window.get-size))
        raise GPUError.OutdatedSwapchain

    let cmd-encoder =
        wgpu.DeviceCreateCommandEncoder istate.device
            (&local wgpu.CommandEncoderDescriptor)

    let render-pass =
        RenderPass cmd-encoder
            arrayof wgpu.RenderPassColorAttachment
                typeinit
                    view = swapchain-image
                    clearColor = (typeinit 0.017 0.017 0.017 1.0)

    _ render-pass

fn present (render-pass)
    wgpu.RenderPassEncoderEndPass render-pass._handle

    local cmd-buf =
        wgpu.CommandEncoderFinish render-pass._cmd-encoder
            (&local wgpu.CommandBufferDescriptor)

    wgpu.QueueSubmit istate.queue 1 &cmd-buf
    wgpu.SwapChainPresent istate.swapchain
    ;

do
    let init begin-frame present bind-buffer
    locals;
