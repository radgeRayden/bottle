using import Array String struct
using import .common ..context .types

import .wgpu
from wgpu let typeinit@ chained@

ctx := context-accessor 'gpu

type+ ColorTarget
    inline... __typecall (cls, format : wgpu.TextureFormat)
        bitcast
            wgpu.ColorTargetState
                format = format
                writeMask = wgpu.ColorWriteMask.All
                blend =
                    typeinit@
                        color =
                            wgpu.BlendComponent
                                operation = 'Add
                                srcFactor = 'SrcAlpha
                                dstFactor = 'OneMinusSrcAlpha
                        alpha =
                            wgpu.BlendComponent
                                operation = 'Add
                                srcFactor = 'One
                                dstFactor = 'OneMinusSrcAlpha
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
                    entryPoint = self.entry-point
                    targetCount = count as u32
                    targets = ptr as (@ wgpu.ColorTargetState)

    inline __toptr (self)
        & (imply self wgpu.FragmentState)

fn make-pipeline-layout (count layouts)
    layouts as:= pointer (storageof wgpu.BindGroupLayout)
    wgpu.DeviceCreatePipelineLayout ctx.device
        typeinit@
            label = "Bottle Pipeline Layout"
            bindGroupLayoutCount = count
            bindGroupLayouts = layouts

type+ PipelineLayout
    inline... __typecall (cls, bind-group-layouts : (Array BindGroupLayout))
        ptr count := 'data bind-group-layouts
        wrap-nullable-object cls
            make-pipeline-layout count (dupe ptr)
    case (cls, bind-group-layouts : (array BindGroupLayout))
        local layouts = bind-group-layouts
        wrap-nullable-object cls
            make-pipeline-layout (countof layouts) &layouts
    case (cls)
        wrap-nullable-object cls
            make-pipeline-layout 0:u32 null

fn make-pipeline (layout topology winding vertex-stage fragment-stage sample-count depth-testing?)
    let depth-stencil-state =
        if depth-testing?
            local state : wgpu.DepthStencilState
                # FIXME: allow a configurable depth format
                format = 'Depth32FloatStencil8
                depthWriteEnabled = true
                depthCompare = 'Less
                # FIXME: configurable stencil state
                stencilFront =
                    typeinit
                        compare = 'Always
                        failOp = 'Zero
                        depthFailOp = 'Zero
                        passOp = 'Zero
                stencilBack =
                    typeinit
                        compare = 'Always
                        failOp = 'Zero
                        depthFailOp = 'Zero
                        passOp = 'Zero
                # FIXME: depth bias stuff missing
            &state
        else null

    local fragment-state = dupe (imply (move fragment-stage) FragmentState)
    wgpu.DeviceCreateRenderPipeline ctx.device
        typeinit@
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
            fragment = &fragment-state as (@ wgpu.FragmentState)
            depthStencil = depth-stencil-state

type+ RenderPipeline
    inline... __typecall (cls,
                          layout         : PipelineLayout,
                          topology       : wgpu.PrimitiveTopology,
                          winding        : wgpu.FrontFace,
                          vertex-stage   : VertexStage,
                          fragment-stage : FragmentStage,
                          msaa-samples : u32 = 1:u32,
                          depth-testing? : bool = false) # TODO: make this configurable

        cls ... := *...
        wrap-nullable-object cls (make-pipeline ...)

    fn... get-bind-group-layout (self, index : u32)
        wrap-nullable-object BindGroupLayout (wgpu.RenderPipelineGetBindGroupLayout (view self) index)

()
