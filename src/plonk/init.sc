using import Array enum glm Map Option String struct hash

# imported to define the implementations
using import .TextureBinding

using import .common ..context ..gpu.types ..enums ..helpers
import ..asset ..gpu ..math ..window .shaders ..time

TEXTURE-CACHE-EVICTION-TIME := 1.0:f64

struct DrawCommand
    offset : usize
    elements : usize
    texture-binding : TextureBinding
    pipeline : RenderPipeline
    transform : mat4

struct StartPassCommand
    render-target : TextureBinding
    clear? : bool
    clear-color : vec4

enum PlonkCommand
    Draw : DrawCommand
    StartPass : StartPassCommand

struct TextureCacheKey plain
    texture-handle : Texture.DumbHandleType
    filter-min : FilterMode
    filter-mag : FilterMode

    # TODO: make some shorthands for this type of thing
    inline __hash (self)
        va-lfold (hash 0)
            (_? a b) -> (hash b (hash (getattr self (keyof a.Type))))
            this-type.__fields__
    inline __== (thisT otherT)
        static-if ((unqualified thisT) == (unqualified otherT))
            inline (self other)
                (storagecast self) == (storagecast other)

struct TextureCacheEntry
    binding : TextureBinding
    timestamp : f64
    key : TextureCacheKey

struct PlonkState
    push-constant-layout : PushConstantLayout
    default-texture : Texture

    attribute-buffer : (StorageBuffer VertexAttributes)
    index-buffer     : (IndexBuffer u32)

    render-pass : (Option RenderPass)
    transform   : mat4
    vertex-data : (Array VertexAttributes)
    index-data  : (Array u32)
    pipeline : RenderPipeline
    buffer-binding : BindGroup
    texture-binding : (Option TextureBinding)
    texture-filter-min : FilterMode
    texture-filter-mag : FilterMode
    render-target : TextureView
    clear-color : vec4

    index-offset : usize

    commands : (Array PlonkCommand)

    cached-textures : (Array TextureCacheEntry)
    cached-texture-map : (Map TextureCacheKey usize)

global ctx : PlonkState

fn push-constant-layout ()
    local push-constant-layout : PushConstantLayout
    'add-range push-constant-layout ShaderStage.Vertex S"transform" mat4
    push-constant-layout

fn get-buffer-binding-layout ()
    gpu.get-internal-bind-group-layout S"plonk.buffer-binding-layout"
        fn ()
            local bg-layout = 'builder BindGroupLayout
            'set-vertex-visibility bg-layout true
            'add-buffer-binding bg-layout 'ReadOnlyStorage
            'finalize bg-layout

fn get-pipeline-layout ()
    gpu.get-internal-pipeline-layout S"plonk.pipeline-layout"
        fn ()
            local pip-layout-entries : (Array BindGroupLayout)
            'append pip-layout-entries (get-buffer-binding-layout)
            'append pip-layout-entries TextureBinding.BindGroupLayout

            PipelineLayout pip-layout-entries (view (push-constant-layout))

fn default-pipeline ()
    gpu.get-internal-pipeline "plonk.default-pipeline"
        fn ()
            vert := ShaderModule shaders.generic-vert ShaderLanguage.SPIRV ShaderStage.Vertex
            frag := ShaderModule shaders.generic-frag ShaderLanguage.SPIRV ShaderStage.Fragment

            pipeline :=
                RenderPipeline
                    layout = (get-pipeline-layout)
                    topology = PrimitiveTopology.TriangleList
                    winding = FrontFace.CCW
                    vertex-stage =
                        VertexStage
                            module = vert
                            entry-point = "main"
                    fragment-stage =
                        FragmentStage
                            module = frag
                            entry-point = "main"
                            color-targets =
                                typeinit
                                    ColorTarget
                                        format = (gpu.get-preferred-surface-format)
                    msaa-samples = (gpu.msaa-enabled?) 4:u32 1:u32

fn default-texture ()
    gpu.get-internal-texture "plonk.default1x1white-texture"
        fn ()
            local imdata = asset.ImageData 1 1
            for byte in imdata.data
                byte = 0xFF
            Texture imdata

fn default-transform ()
    w h := (window.get-drawable-size)
    *
        math.orthographic-projection w h
        math.translation-matrix (vec3 (-w / 2) (-h / 2) 0)

fn finalize-enqueue-command (ctx)
    elements := (countof ctx.index-data) - ctx.index-offset
    'append ctx.commands
        PlonkCommand.Draw
            typeinit
                copy ctx.index-offset
                elements
                copy ('force-unwrap ctx.texture-binding)
                copy ctx.pipeline
                copy ctx.transform
    ctx.index-offset = countof ctx.index-data

fn... set-texture-filtering (filter-min : FilterMode = 'Nearest, filter-mag : FilterMode = 'Nearest)
    ctx.texture-filter-min = filter-min
    ctx.texture-filter-mag = filter-mag

fn... set-texture-binding (ctx, texture : Texture, filter-min : FilterMode, filter-mag : FilterMode)
    # bind groups and texture views are created on demand and cached.
    :: get-texture-cache-entry
    k := TextureCacheKey ('get-id texture) filter-min filter-mag
    now := (time.get-raw-time)
    try ('get ctx.cached-texture-map k)
    then (index)
        entry := ctx.cached-textures @ index 
        entry.timestamp = now
        entry
    else
        'set ctx.cached-texture-map k (countof ctx.cached-textures)
        'append ctx.cached-textures
            TextureCacheEntry
                binding =
                    # FIXME: needs configurable wrap mode
                    TextureBinding (copy texture) 'ClampToEdge filter-min filter-mag
                timestamp = now
                key = k
    get-texture-cache-entry (entry) ::

    # FIXME: this check doesn't trigger a cache invalidation if only filtering changes
    if (('get-key ('force-unwrap ctx.texture-binding)) != ('get-key entry.binding))
        finalize-enqueue-command ctx
    ctx.texture-binding = copy entry.binding

fn... set-pipeline (pipeline : RenderPipeline)
    if (('get-key ctx.pipeline) != ('get-key pipeline))
        finalize-enqueue-command ctx
    ctx.pipeline = pipeline

fn... set-transform (transform : mat4)
    finalize-enqueue-command ctx
    ctx.transform = transform

fn... set-render-target (render-target : (param? TextureBinding) = none, clear? : bool = true, clear-color : vec4 = (vec4))
    let render-target =
        static-if (not (none? render-target))
            render-target
        else (gpu.get-surface-texture)

    if (('get-key ctx.render-target) != ('get-key render-target))
        'append ctx.commands
            PlonkCommand.StartPass
                typeinit
                    copy render-target
                    clear?
                    clear-color

    ctx.render-target = render-target

fn... add-quad (ctx, position : vec2, size : vec2, rotation : f32 = 0:f32, quad : Quad = (Quad (vec2 0 0) (vec2 1 1)),
                origin : vec2 = (vec2 0.5), fliph? : bool = false, flipv? : bool = false, color : vec4 = (vec4 1))
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
        v := ((norm-vertices @ vertex-index) - origin) * size
        VertexAttributes
            position = (position + (math.rotate2D v rotation))
            texcoords = (quad.start + ((texcoords @ uv-index) * quad.extent))
            color = color

    vtx-offset := u32 (countof ctx.vertex-data)

    inline get-idx (input)
        input as:= u8
        # fliph makes index switch from odd to even (toggle bit 0)
        # flipv swaps 0,1 <-> 2,3 (toggle bit 1)
        output := bxor input (u8 fliph?)
        output := bxor output ((u8 flipv?) << 1)
        _ input output

    'append ctx.vertex-data
        make-vertex (get-idx 0)
    'append ctx.vertex-data
        make-vertex (get-idx 1)
    'append ctx.vertex-data
        make-vertex (get-idx 2)
    'append ctx.vertex-data
        make-vertex (get-idx 3)

    'append ctx.index-data (0:u32 + vtx-offset)
    'append ctx.index-data (2:u32 + vtx-offset)
    'append ctx.index-data (3:u32 + vtx-offset)
    'append ctx.index-data (3:u32 + vtx-offset)
    'append ctx.index-data (1:u32 + vtx-offset)
    'append ctx.index-data (0:u32 + vtx-offset)
    ;

fn... sprite (texture : Texture, position : vec2, size : vec2, rotation : f32 = 0:f32, quad : Quad = (Quad (vec2 0 0) (vec2 1 1)),
                origin : vec2 = (vec2 0.5), fliph? : bool = false, flipv? : bool = false, color : vec4 = (vec4 1))
    texture ... := *...
    set-texture-binding ctx texture ctx.texture-filter-min ctx.texture-filter-mag
    add-quad ctx ...

fn... rectangle (position : vec2, size : vec2, rotation : f32 = 0, color : vec4 = (vec4 1))
    set-texture-binding ctx ctx.default-texture
    add-quad ctx position size rotation (color = color)

fn regular-polygon-point (center radius idx segments rotation-offset)
    angle := (f32 idx) * (2 * pi / (f32 segments)) + (pi / 2) + rotation-offset
    center + ((vec2 (cos angle) (sin angle)) * radius)

fn... polygon (center : vec2, segments : integer, radius : f32, rotation : f32 = 0:f32, color : vec4 = (vec4 1))
    set-texture-binding ctx ctx.default-texture
    vtx-offset := u32 (countof ctx.vertex-data)

    segments := (max (u32 segments) 3:u32)

    'append ctx.vertex-data
        VertexAttributes (position = center) (color = color)
    for i in (range (segments + 1))
        'append ctx.vertex-data
            VertexAttributes
                position = (regular-polygon-point center radius i segments rotation)
                color = color
    for i in (range (segments + 1))
        'append ctx.index-data vtx-offset # center
        'append ctx.index-data (vtx-offset + i)
        'append ctx.index-data (vtx-offset + i + 1)
    ()

fn calculate-circle-segment-count (radius)
    radius as:= f32
    errorv := 0.33
    segments := math.ceil (pi / (acos (1 - errorv / (max radius errorv))))
    max 5:u32 (u32 segments)

fn... circle (center : vec2, radius : f32, color : vec4 = (vec4 1), segments : (param? i32) = none)
    set-texture-binding ctx ctx.default-texture

    let segments =
        static-if (none? segments) (calculate-circle-segment-count radius)
        else segments

    polygon center segments radius 0:f32 color
    ()

fn... line (vertices, width : f32 = 1.0, color : vec4 = (vec4 1),
            join-kind : LineJoinKind = LineJoinKind.Bevel,
            cap-kind : LineCapKind = LineCapKind.Butt)
    set-texture-binding ctx ctx.default-texture

    if ((countof vertices) < 2)
        return;

    local segment-vertices =
        arrayof vec2
            vec2 (-0.5,  1.0) #tl
            vec2 ( 0.5,  1.0) #tr
            vec2 (-0.5,  0.0) #bl
            vec2 ( 0.5,  0.0) #br

    inline next-index ()
        u32 (countof ctx.vertex-data)

    for i in (range ((countof vertices) - 1))
        start end := vertices @ i, vertices @ (i + 1)
        inline make-vertex (idx)
            dir := end - start
            perp := normalize (vec2 dir.y -dir.x)

            vertex := segment-vertices @ idx
            vpos := start + (dir * vertex.y) + (perp * vertex.x * width)
            VertexAttributes
                position = vpos
                color = color

        first-index := (next-index)
        idx := (i) -> ((u32 i) + first-index)
        add-idx := (i) -> ('append ctx.index-data (idx i))
        add-vtx := (v) -> ('append ctx.vertex-data v)

        va-map
            inline (i)
                add-vtx (make-vertex i)
            _ 0 1 2 3

        va-map add-idx
            _ 0 2 3 3 1 0

    join-range := (range 1 ((countof vertices) - 1))
    switch join-kind
    case LineJoinKind.Bevel
        for i in join-range
            a-start ljoin b-end := vertices @ (i - 1), vertices @ i, vertices @ (i + 1)
            first-index := (next-index)
            idx := (i) -> ((u32 i) + first-index)
            add-idx := (i) -> ('append ctx.index-data (idx i))
            add-vtx := (v) -> ('append ctx.vertex-data v)

            inline get-perpendicular (start end)
                dir := end - start
                normalize (vec2 dir.y -dir.x)

            perp-A perp-B := get-perpendicular a-start ljoin, get-perpendicular ljoin b-end
            tangent := perp-B - perp-A
            normal := normalize (vec2 tangent.y -tangent.x)
            sigma := sign (dot (perp-A + perp-B) normal)

            va-map
                inline (vpos)
                    add-vtx
                        VertexAttributes
                            position = vpos
                            color = color
                _
                    ljoin + (perp-A * (width / 2) * sigma)
                    ljoin
                    ljoin + (perp-B * (width / 2) * sigma)
            va-map add-idx
                _ 0 1 2
    case LineJoinKind.Miter
    case LineJoinKind.Round
        for i in join-range
            # TODO: change to semicircle when that is a thing
            circle (vertices @ i) (width / 2) color
    default ()

    start end := vertices @ 0, vertices @ ((countof vertices) - 1)
    switch cap-kind
    case LineCapKind.Square
        radius := (width / 2) * (static-eval (sqrt 2:f32))
        start+1 end-1 := vertices @ 1, vertices @ ((countof vertices) - 2)
        sangle eangle := atan2 (unpack ((start+1 - start) . yx)), atan2 (unpack ((end - end-1) . yx))
        polygon start 4 radius (sangle - (pi / 4)) color
        polygon end 4 radius (eangle - (pi / 4)) color
    case LineCapKind.Round
        # TODO: change to semicircle when that is a thing
        circle start (width / 2) color
        circle end (width / 2) color
    default ()

fn... rectangle-line (position : vec2, size : vec2,
                      rotation : f32 = 0:f32, origin : vec2 = (vec2 0.5), line-width : f32 = 1:f32, color : vec4 = (vec4 1),
                      join-kind : LineJoinKind = LineJoinKind.Bevel,
                      cap-kind : LineCapKind = LineCapKind.Butt)
    local vertices = (array vec2 5)
        va-map
            inline (v)
                position + (math.rotate2D ((v - origin) * size) rotation)
            _
                vec2 0 1 # top left
                vec2 1 1 # top right
                vec2 1 0 # bottom right
                vec2 0 0 # bottom left
                vec2 0 1 # top left
    line vertices line-width color join-kind cap-kind
    ()

fn... polygon-line (center, segments, radius, rotation,
                        line-width : f32 = 1:f32, color : vec4 = (vec4 1),
                        join-kind : LineJoinKind = LineJoinKind.Bevel,
                        cap-kind : LineCapKind = LineCapKind.Butt)
    radius as:= f32
    segments := (max (u32 segments) 3:u32)
    # FIXME: maybe we can avoid this allocation in the future
    local line-vertices : (Array vec2)
    for i in (range (segments + 1))
        'append line-vertices (regular-polygon-point center radius i segments rotation)
    line line-vertices line-width color join-kind cap-kind
    ()

fn... circle-line (center, radius, color : vec4 = (vec4 1),
                        segments : (param? i32) = none, line-width : f32 = 1:f32,
                        join-kind : LineJoinKind = LineJoinKind.Bevel,
                        cap-kind : LineCapKind = LineCapKind.Butt)
    let segments =
        static-if (none? segments) (calculate-circle-segment-count radius)
        else segments

    polygon-line center segments radius 0:f32 line-width color join-kind cap-kind
    ()

@@ if-module-enabled 'plonk
fn init ()
    try # none of this is supposed to fail. If it does, we will crash as we should when trying to unwrap state.
        attrbuf := (StorageBuffer VertexAttributes) 4096
        buffer-binding := BindGroup (get-buffer-binding-layout) (view attrbuf)
        ctx =
            PlonkState
                push-constant-layout = (push-constant-layout)
                default-texture = (default-texture)
                attribute-buffer = attrbuf
                index-buffer = typeinit 8192
                transform = (default-transform)
                pipeline = copy (default-pipeline)
                buffer-binding = buffer-binding
                texture-binding = (TextureBinding (default-texture))
                clear-color = (vec4 0.017 0.017 0.017 1.0)
                render-target = dupe (nullof TextureView)
    else ()

@@ if-module-enabled 'plonk
fn begin-frame ()
    ctx.render-target = copy (gpu.get-surface-texture)
    ctx.transform = (default-transform)

    # trim texture cache
    now := (time.get-raw-time)
    for idx in (rrange (countof ctx.cached-textures))
        entry := ctx.cached-textures @ idx
        old-key := entry.key
        if ((now - entry.timestamp) > TEXTURE-CACHE-EVICTION-TIME)
            entry = 'pop ctx.cached-textures
            'set ctx.cached-texture-map entry.key idx
            'discard ctx.cached-texture-map old-key
    ()

@@ if-module-enabled 'plonk
fn submit ()
    if (ctx.index-offset < (countof ctx.index-data))
        finalize-enqueue-command ctx

    inline next-power-of-two (x)
        1:u32 << (u32 (math.ceil (log2 (f32 x))))

    # NOTE: currently we have no precautions against the buffer getting too big. Ideally
    # we would have arrays of buffers that can handle all the data we throw at it. In practice limits
    # are really high, however.
    vertex-count := countof ctx.vertex-data
    if (vertex-count > ctx.attribute-buffer.Capacity)
        ctx.attribute-buffer = typeinit (next-power-of-two vertex-count)
        ctx.buffer-binding = BindGroup (get-buffer-binding-layout) ctx.attribute-buffer

    index-count := countof ctx.index-data
    if (index-count > ctx.index-buffer.Capacity)
        ctx.index-buffer = typeinit (next-power-of-two index-count)

    'frame-write ctx.attribute-buffer ctx.vertex-data
    'frame-write ctx.index-buffer ctx.index-data
    'clear ctx.vertex-data
    'clear ctx.index-data
    ctx.index-offset = 0

    cmd-encoder := (gpu.get-cmd-encoder)
    surface-texture := (gpu.get-surface-texture)
    let rp =
        if (not (gpu.msaa-enabled?))
            RenderPass cmd-encoder (ColorAttachment surface-texture none false)
        else
            resolve-source := (gpu.get-msaa-resolve-source)
            RenderPass cmd-encoder (ColorAttachment resolve-source surface-texture false)

    vvv bind final-pass
    fold (current-pass pipeline tbinding = (copy rp) (nullof RenderPipeline) (nullof BindGroup)) \
    for cmd in ctx.commands
        dispatch cmd
        case Draw (cmd)
            if (cmd.pipeline != pipeline)
                'set-pipeline current-pass cmd.pipeline
            'set-index-buffer current-pass ctx.index-buffer
            'set-bind-group current-pass 0 ctx.buffer-binding
            if (cmd.texture-binding.bind-group != tbinding)
                'set-bind-group current-pass 1 cmd.texture-binding.bind-group
            'set-push-constant current-pass ctx.push-constant-layout 0 cmd.transform
            'draw-indexed current-pass (u32 cmd.elements) 1:u32 (u32 cmd.offset)
            _ current-pass (deref cmd.pipeline) (deref cmd.texture-binding.bind-group)
        # case StartPass (cmd)
            # 'finish current-pass
            # _
            #     RenderPass cmd-encoder (ColorAttachment cmd.render-target none cmd.clear? cmd.clear-color)
            #     nullof RenderPipeline
            #     nullof BindGroup
        default
            abort;

    'finish rp
    'clear ctx.commands
    ()

do
    let init begin-frame sprite rectangle rectangle-line circle circle-line polygon polygon-line line submit
    let set-texture-filtering
    let Quad LineJoinKind LineCapKind TextureBinding
    local-scope;
