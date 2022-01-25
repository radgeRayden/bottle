using import struct

using import ..helpers

import ..window

let sdl = (import ..FFI.sdl)
let wgpu = (import ..FFI.wgpu)

using import .istate

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

    out vcolor : vec4
        location = 0

    local vertices =
        #   0
        #  /  \
        # 1----2
        arrayof vec3
            vec3  0.0  0.5 0.0
            vec3 -0.5 -0.5 0.0
            vec3  0.5 -0.5 0.0

    local colors =
        arrayof vec4
            vec4 1 0 0 1
            vec4 0 1 0 1
            vec4 0 0 1 1

    gl_Position = (vec4 (vertices @ gl_VertexIndex) 1)
    vcolor = (colors @ gl_VertexIndex)

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

fn make-default-pipeline ()
    let vertex-module fragment-module =
        shader-module-from-SPIRV
            static-compile-spirv 0x10000 'vertex (static-typify vshader)
        shader-module-from-SPIRV
            static-compile-spirv 0x10000 'fragment (static-typify fshader)

    let pip-layout =
        wgpu.DeviceCreatePipelineLayout istate.device
            (&local wgpu.PipelineLayoutDescriptor)

    wgpu.DeviceCreateRenderPipeline istate.device
        &local wgpu.RenderPipelineDescriptor
            label = "my shiny pipeline"
            layout = pip-layout
            vertex =
                wgpu.VertexState
                    module = vertex-module
                    entryPoint = "main"
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

    istate.swapchain = (create-swapchain (window.get-size))
    istate.queue = (wgpu.DeviceGetQueue istate.device)

    let pip = (make-default-pipeline)
    istate.default-pipeline = pip
    ;

fn present ()
    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)
    if (swapchain-image == null)
        istate.swapchain = (create-swapchain (window.get-size))
        return;

    let cmd-encoder =
        wgpu.DeviceCreateCommandEncoder istate.device
            (&local wgpu.CommandEncoderDescriptor)

    let render-pass =
        wgpu.CommandEncoderBeginRenderPass cmd-encoder
            &local wgpu.RenderPassDescriptor
                label = "my render pass"
                colorAttachmentCount = 1
                colorAttachments =
                    &local wgpu.RenderPassColorAttachment
                        view = swapchain-image
                        clearColor = (typeinit 0.017 0.017 0.017 1.0)

    wgpu.RenderPassEncoderSetPipeline render-pass istate.default-pipeline
    wgpu.RenderPassEncoderDraw render-pass 3 1 0 0

    wgpu.RenderPassEncoderEndPass render-pass

    local cmd-buf =
        wgpu.CommandEncoderFinish cmd-encoder
            (&local wgpu.CommandBufferDescriptor)

    wgpu.QueueSubmit istate.queue 1 &cmd-buf
    wgpu.SwapChainPresent istate.swapchain
    ;

do
    let init present
    locals;
