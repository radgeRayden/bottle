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

struct BindGroupLayoutEntry


type BindGroupLayout <:: wgpu.BindGroupLayout
    inline... __typecall (cls, entries)

fn make-pipeline-layout (count layouts)
    layouts as:= pointer (storageof wgpu.BindGroupLayout)
    wgpu.DeviceCreatePipelineLayout istate.device
        &local wgpu.PipelineLayoutDescriptor
            label = "Bottle Pipeline Layout"
            bindGroupLayoutCount = count
            bindGroupLayouts = layouts

type PipelineLayout <:: wgpu.PipelineLayout
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
                    mask = (~ 0:u32)
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
        bitcast (make-pipeline ...) cls

do
    let BindGroupLayout
        BindGroupLayoutEntry
        ColorTarget
        PipelineLayout
        RenderPipeline
        FragmentStage
        VertexStage
    local-scope;
