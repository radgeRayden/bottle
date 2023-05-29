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
                writeMask = wgpu.ColorWriteMask.All
            cls

type VertexStage <: wgpu.VertexState
    inline... __typecall (cls, shader : ShaderModule, entry-point : String)
        bitcast
            wgpu.VertexState
                module = (view shader)
                entryPoint = entry-point
            cls

    inline __imply (this other)
        static-if (other == (superof this-type))
            inline (self)
                bitcast self other

type FragmentStage <: wgpu.FragmentState
    inline... __typecall (cls, shader : ShaderModule, entry-point : String, color-targets)
        count ptr := array->ptr color-targets
        bitcast
            wgpu.FragmentState
                module = (view shader)
                entryPoint = entry-point
                targetCount = count as u32
                targets = ptr as (@ wgpu.ColorTargetState)
            cls

    inline __imply (this other)
        static-if (other == (superof this-type))
            inline (self)
                bitcast self other


type BindGroupLayout <:: wgpu.BindGroupLayout
fn make-pipeline-layout (count layouts)
    layouts as:= pointer (storageof wgpu.BindGroupLayout)
    wgpu.DeviceCreatePipelineLayout istate.device
        &local wgpu.PipelineLayoutDescriptor
            label = "Bottle Pipeline Layout"
            bindGroupLayoutCount = count
            bindGroupLayouts = layouts

type PipelineLayout <:: wgpu.PipelineLayout
    inline... (cls, bind-group-layouts : (Array BindGroupLayout))
        wrap-nullable-object cls
            make-pipeline-layout (countof bind-group-layouts) (imply bind-group-layouts pointer)
    case (cls, bind-group-layouts : (array BindGroupLayout))
        local layouts = bind-group-layouts
        wrap-nullable-object cls
            make-pipeline-layout (countof layouts) &layouts
    case (cls)
        wrap-nullable-object cls
            make-pipeline-layout 0:u32 null

fn make-pipeline (layout topology winding vertex-stage fragment-stage)
    wgpu.DeviceCreateRenderPipeline istate.device
        &local wgpu.RenderPipelineDescriptor
            label = "Bottle Render Pipeline"
            layout = layout
            vertex = vertex-stage
            primitive =
                wgpu.PrimitiveState
                    topology = topology
                    frontFace = winding
            multisample =
                wgpu.MultisampleState
                    count = 1
                    mask = ~0:u32

                    alphaToCoverageEnabled = false
            fragment = (&local (imply fragment-stage (superof fragment-stage)))

type RenderPipeline <:: wgpu.RenderPipeline
    inline... __typecall (cls,
                          layout         : PipelineLayout,
                          topology       : wgpu.PrimitiveTopology,
                          winding        : wgpu.FrontFace,
                          vertex-stage   : VertexStage,
                          fragment-stage : FragmentStage)

        cls ... := *...
        wrap-nullable-object cls (make-pipeline ...)

do
    let BindGroupLayout
        ColorTarget
        PipelineLayout
        RenderPipeline
        FragmentStage
        VertexStage
    local-scope;
