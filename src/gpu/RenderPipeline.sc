using import Array
using import String
using import struct

import .wgpu
using import .common
using import ..helpers
using import .types

type+ ColorTarget
    inline... __typecall (cls, format : wgpu.TextureFormat)
        bitcast
            wgpu.ColorTargetState
                format = format
                writeMask = wgpu.ColorWriteMask.All
                blend =
                    &local wgpu.BlendState
                        color =
                            wgpu.BlendComponent
                                operation = wgpu.BlendOperation.Add
                                srcFactor = wgpu.BlendFactor.SrcAlpha
                                dstFactor = wgpu.BlendFactor.OneMinusSrcAlpha
                        alpha =
                            wgpu.BlendComponent
                                operation = wgpu.BlendOperation.Add
                                srcFactor = wgpu.BlendFactor.One
                                dstFactor = wgpu.BlendFactor.OneMinusSrcAlpha
            cls

    inline __imply (thisT otherT)
        static-if (otherT == wgpu.ColorTargetState)
            inline (self)
                bitcast self otherT

type VertexState <:: wgpu.VertexState
type FragmentState <:: wgpu.FragmentState
type+ VertexStage
    inline __imply (thisT otherT)
        static-if (otherT == VertexState)
            inline (self)
                VertexState
                    module = self.module
                    entryPoint = self.entry-point

type+ FragmentStage
    inline __imply (thisT otherT)
        static-if (otherT == FragmentState)
            inline (self)
                ptr count := 'data self.color-targets
                FragmentState
                    module = self.module
                    entryPoint = (dump self.entry-point)
                    targetCount = count as u32
                    targets = ptr as (@ wgpu.ColorTargetState)

    inline __toptr (self)
        & (imply self wgpu.FragmentState)

fn make-pipeline-layout (count layouts)
    layouts as:= pointer (storageof wgpu.BindGroupLayout)
    wgpu.DeviceCreatePipelineLayout istate.device
        &local wgpu.PipelineLayoutDescriptor
            label = "Bottle Pipeline Layout"
            bindGroupLayoutCount = count
            bindGroupLayouts = layouts

type+ PipelineLayout
    inline... __typecall (cls, bind-group-layouts : (Array BindGroupLayout))
        wrap-nullable-object cls
            make-pipeline-layout (countof bind-group-layouts) (imply bind-group-layouts pointer)
    case (cls, bind-group-layouts : (array BindGroupLayout))
        local layouts = bind-group-layouts
        wrap-nullable-object cls
            make-pipeline-layout (countof layouts) &layouts
    case (cls)
        wrap-nullable-object cls
            make-pipeline-layout 0:u32 null

fn make-pipeline (layout topology winding vertex-stage fragment-stage sample-count)
    wgpu.DeviceCreateRenderPipeline istate.device
        &local wgpu.RenderPipelineDescriptor
            label = "Bottle Render Pipeline"
            layout = layout
            vertex = (dupe (bitcast (imply (move vertex-stage) VertexState) wgpu.VertexState))
            primitive =
                wgpu.PrimitiveState
                    topology = topology
                    frontFace = winding
            multisample =
                wgpu.MultisampleState
                    count = sample-count
                    mask = ~0:u32
                    alphaToCoverageEnabled = false
            fragment = (&local (dupe (imply (move fragment-stage) FragmentState))) as (@ wgpu.FragmentState)

type+ RenderPipeline
    inline... __typecall (cls,
                          layout         : PipelineLayout,
                          topology       : wgpu.PrimitiveTopology,
                          winding        : wgpu.FrontFace,
                          vertex-stage   : VertexStage,
                          fragment-stage : FragmentStage,
                          msaa-samples : u32 = 1:u32)

        cls ... := *...
        wrap-nullable-object cls (make-pipeline ...)

    fn... get-bind-group-layout (self, index : u32)
        wrap-nullable-object BindGroupLayout (wgpu.RenderPipelineGetBindGroupLayout (view self) index)

()
