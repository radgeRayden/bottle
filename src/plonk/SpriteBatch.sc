using import Array
using import glm
using import Option
using import struct

import ..math
using import ..gpu.types
using import .common

struct SpriteBatch
    attribute-buffer : (StorageBuffer VertexAttributes)
    index-buffer     : (IndexBuffer u32)
    uniform-buffer   : (UniformBuffer Uniforms)
    vertex-data : (Array VertexAttributes)
    index-data  : (Array u32)
    outdated-vertices? : bool = true
    outdated-indices?  : bool = true
    vertex-offset : usize
    index-offset  : usize

    pipeline : RenderPipeline
    bind-group : (Option BindGroup)
    cached-buffer-id : u64

    fn flush (self render-pass)
        if (not (self.outdated-vertices? or self.outdated-indices?))
            return;

        if self.outdated-vertices?
            attrbuf := self.attribute-buffer
            try
                'frame-write attrbuf self.vertex-data self.vertex-offset
            else
                # resize then try again
                self.attribute-buffer = ('clone attrbuf (attrbuf.Capacity * 2:usize))
                return (this-function self render-pass)

            if (self.cached-buffer-id != ('get-id attrbuf))
                self.bind-group = BindGroup ('get-bind-group-layout self.pipeline 0) attrbuf self.uniform-buffer
                self.cached-buffer-id = ('get-id attrbuf)

            self.outdated-vertices? = false

        if self.outdated-indices?
            idxbuf := self.index-buffer
            try
                'frame-write idxbuf self.index-data self.index-offset
            else
                # resize then try again
                self.index-buffer = ('clone idxbuf (idxbuf.Capacity * 2:usize))
                return (this-function self render-pass)

            self.outdated-indices? = false

        index-count := (countof self.index-data)
        'set-index-buffer render-pass self.index-buffer
        'set-pipeline render-pass self.pipeline
        'set-bind-group render-pass 0 ('force-unwrap self.bind-group)
        'draw-indexed render-pass (index-count as u32) 1:u32 (u32 self.index-offset)

        self.vertex-offset += (countof self.vertex-data)
        self.index-offset  += (countof self.index-data)
        'clear self.vertex-data
        'clear self.index-data

    fn... add-sprite (self : this-type, position, size, rotation : f32, quad = (Quad (vec2 0 0) (vec2 1 1)),
                      fliph? = false, flipv? = false, color = (vec4 1))

        self.outdated-vertices? = true
        self.outdated-indices? = true

        local norm-vertices =
            # 0 - 1
            # | \ |
            # 2 - 3
            arrayof vec2
                vec2 0 1 # top left
                vec2 1 1 # top right
                vec2 0 0 # bottom left
                vec2 1 0 # bottom right

        local texcoords =
            arrayof vec2
                vec2 0 0 # top left
                vec2 1 0 # top right
                vec2 0 1 # bottom left
                vec2 1 1 # bottom right

        inline make-vertex (vertex-index uv-index)
            vertex :=
                +
                    math.rotate2D
                        (norm-vertices @ vertex-index) - (vec2 0.5)
                        rotation
                    vec2 0.5

            VertexAttributes
                position = (position + (vertex * size))
                texcoords = (quad.start + ((texcoords @ uv-index) * quad.extent))
                color = color

        # position in the batched vertex buffer + current vertex in this batch
        vtx-offset := self.vertex-offset + (countof self.vertex-data)
        vtx-offset as:= u32

        inline get-idx (input)
            input as:= u8
            # fliph makes index switch from odd to even (toggle bit 0)
            # flipv swaps 0,1 <-> 2,3 (toggle bit 1)
            output := bxor input (u8 fliph?)
            output := bxor output ((u8 flipv?) << 1)
            _ input output

        'append self.vertex-data
            make-vertex (get-idx 0)
        'append self.vertex-data
            make-vertex (get-idx 1)
        'append self.vertex-data
            make-vertex (get-idx 2)
        'append self.vertex-data
            make-vertex (get-idx 3)

        'append self.index-data (0:u32 + vtx-offset)
        'append self.index-data (2:u32 + vtx-offset)
        'append self.index-data (3:u32 + vtx-offset)
        'append self.index-data (3:u32 + vtx-offset)
        'append self.index-data (1:u32 + vtx-offset)
        'append self.index-data (0:u32 + vtx-offset)
        ;

    fn finish (self render-pass)
        'flush self render-pass
        self.vertex-offset = 0
        self.index-offset = 0

do
    let SpriteBatch
    local-scope;
