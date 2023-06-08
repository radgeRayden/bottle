using import Array
using import glm
using import struct

using import ...gpu.types
using import .common

struct Quad plain
    start : vec2
    extent : vec2

struct SpriteBatch
    attribute-buffer : (StorageBuffer VertexAttributes)
    index-buffer     : (IndexBuffer u32)
    vertex-data : (Array VertexAttributes)
    index-data  : (Array u32)
    outdated-vertices? : bool
    outdated-indices?  : bool
    vertex-offset : usize
    index-offset  : usize

    fn flush (self render-pass)
        if self.outdated-vertices?
            attrbuf := self.attribute-buffer
            try
                'frame-write attrbuf self.vertex-data self.vertex-offset
            else
                # resize then try again
                self.attribute-buffer = ('clone attrbuf (attrbuf.Capacity * 2))
                return (this-function self)

            self.outdated-vertices? = false

        if self.oudated-indices?
            idxbuf := self.index-buffer
            try
                'frame-write idxbuf self.index-data self.index-offset
            else
                # resize then try again
                self.index-buffer = ('clone idxbuf (idxbuf.Capacity * 2))
                return (this-function self)

            self.outdated-indices? = false

        index-count := (countof self.index-data)
        'set-index-buffer render-pass self.index-buffer
        'draw-indexed render-pass (index-count as u32) 1:u32 (u32 self.index-offset)

        self.vertex-offset += (countof self.vertex-data)
        self.index-offset  += (countof self.index-data)
        'clear self.vertex-data
        'clear self.index-data

    fn... add-sprite (self : this-type, position, size, quad = (Quad (vec2 0 0) (vec2 1 1)), color = (vec4 1))
        self.dirty? = true

        local norm-vertices =
            # 0 - 3
            # | \ |
            # 1 - 2
            arrayof vec2
                vec2 0 1 # top left
                vec2 0 0 # bottom left
                vec2 1 0 # bottom right
                vec2 1 1 # top right

        local texcoords =
            arrayof vec2
                vec2 0 0 # top left
                vec2 0 1 # bottom left
                vec2 1 1 # bottom right
                vec2 1 0 # top right

        inline make-vertex (i)
            VertexAttributes
                position = (position + ((norm-vertices @ i) * size))
                texcoords = (quad.start + ((texcoords @ i) * quad.extent))
                color = color

        let idx-offset = ((countof self.vertex-data) as u16)
        'append self.vertex-data
            make-vertex 0
        'append self.vertex-data
            make-vertex 1
        'append self.vertex-data
            make-vertex 2
        'append self.vertex-data
            make-vertex 3

        'append self.index-data (0:u16 + idx-offset)
        'append self.index-data (1:u16 + idx-offset)
        'append self.index-data (2:u16 + idx-offset)
        'append self.index-data (2:u16 + idx-offset)
        'append self.index-data (3:u16 + idx-offset)
        'append self.index-data (0:u16 + idx-offset)
        ;

    fn finish (self)
        'clear self.vertex-data
        'clear self.index-data
        self.vertex-offset = 0
        self.index-offset = 0

do
    let SpriteBatch
    local-scope;
