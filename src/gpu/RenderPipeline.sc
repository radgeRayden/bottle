using import Array
using import String
using import struct

import .wgpu
using import .common
using import ..helpers
using import .ShaderModule

run-stage;

type ColorTarget <: wgpu.ColorTargetState
    inline... __typecall (cls, format : wgpu.TextureFormat)
        bitcast
            wgpu.ColorTargetState
                format = format
                writeMask = ~0:u32
            cls

type VertexStage <: wgpu.VertexState
    inline... __typecall (cls, shader : ShaderModule, entry-point : String)
        bitcast
            wgpu.VertexState
                module = (storagecast (view shader))
                entryPoint = entry-point
            cls

type FragmentStage <: wgpu.FragmentState
    inline... __typecall (cls, shader : ShaderModule, entry-point : String, color-targets)
        count ptr := array->ptr color-targets
        bitcast
            wgpu.FragmentState
                module = (storagecast (view shader))
                entryPoint = entry-point
                targetCount = count as u32
                targets = ptr as (@ wgpu.ColorTargetState)
            cls

struct BindGroupLayoutEntry


copy-type BindGroupLayout wgpu.BindGroupLayout
    inline... __typecall (cls, entries)

fn make-pipeline-layout (count layouts)
    layouts as:= pointer (storageof wgpu.BindGroupLayout)
    wgpu.DeviceCreatePipelineLayout istate.device
        &local wgpu.PipelineLayoutDescriptor
            label = "Bottle Pipeline Layout"
            bindGroupLayoutCount = count
            bindGroupLayouts = layouts

copy-type PipelineLayout wgpu.PipelineLayout
    inline... __typecall (cls, layout : wgpu.PipelineLayout)
        bitcast layout cls
    case (cls, bind-group-layouts : (Array BindGroupLayout))
        this-function cls
            make-pipeline-layout (countof bind-group-layouts) (imply bind-group-layouts pointer)
    case (cls, bind-group-layouts : (array BindGroupLayout))
        local layouts = bind-group-layouts
        this-function cls
            make-pipeline-layout (countof layouts) &layouts
    case (cls)
        this-function cls
            make-pipeline-layout 0:u32 null

fn make-pipeline (layout topology winding vertex-stage fragment-stage)
    # let pipeline =
    #     wgpu.DeviceCreateRenderPipeline istate.device
    #         &local wgpu.RenderPipelineDescriptor
    #             label = "Bottle Render Pipeline"
    #             layout = pip-layout
    #             vertex =
    #                 wgpu.VertexState
    #                     module = vertex-module
    #                     entryPoint = "vs_main"
    #             primitive =
    #                 wgpu.PrimitiveState
    #                     topology = wgpu.PrimitiveTopology.TriangleList
    #                     frontFace = wgpu.FrontFace.CCW
    #             multisample =
    #                 wgpu.MultisampleState
    #                     count = 1
    #                     mask = (~ 0:u32)
    #                     alphaToCoverageEnabled = false
    #             fragment =
    #                 &local wgpu.FragmentState
    #                     module = fragment-module
    #                     entryPoint = "fs_main"
    #                     targetCount = 1
    #                     targets =
    #                         &local wgpu.ColorTargetState
    #                             format = (get-preferred-surface-format)
    #                             blend =
    #                                 &local wgpu.BlendState
    #                                     color =
    #                                         typeinit
    #                                             srcFactor = wgpu.BlendFactor.SrcAlpha
    #                                             dstFactor = wgpu.BlendFactor.OneMinusSrcAlpha
    #                                             operation = wgpu.BlendOperation.Add
    #                                     alpha =
    #                                         typeinit
    #                                             srcFactor = wgpu.BlendFactor.SrcAlpha
    #                                             dstFactor = wgpu.BlendFactor.OneMinusSrcAlpha
    #                                             operation = wgpu.BlendOperation.Add
    #                             writeMask = wgpu.ColorWriteMask.All

    bitcast null wgpu.RenderPipeline

copy-type RenderPipeline wgpu.RenderPipeline
    inline... __typecall (cls,
                          layout         : PipelineLayout,
                          topology       : wgpu.PrimitiveTopology,
                          winding        : wgpu.FrontFace,
                          vertex-stage   : VertexStage,
                          fragment-stage : FragmentStage)

        cls ... := *...

        bitcast
            make-pipeline ...
            cls

do
    let BindGroupLayout
        BindGroupLayoutEntry
        ColorTarget
        PipelineLayout
        RenderPipeline
        FragmentStage
        VertexStage
    local-scope;
