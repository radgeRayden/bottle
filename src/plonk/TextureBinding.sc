using import struct ..types ..enums String radl.strfmt
import ..gpu

fn get-bind-group-layout ()
    let layout =
        gpu.get-internal-bind-group-layout S"plonk.texture-binding-layout"
            fn ()
                local bg-layout = 'builder BindGroupLayout
                'set-fragment-visibility bg-layout true
                'add-sampler-binding bg-layout
                'add-texture-binding bg-layout
                'finalize bg-layout

fn create-bind-group (texture-view sampler)
    local bind-group = 'builder BindGroup
    'set-layout bind-group (get-bind-group-layout)
    'add-entry bind-group sampler
    'add-entry bind-group texture-view
    'finalize bind-group

type+ TextureBinding
    inline... __typecall (cls, texture : Texture, wrap-mode : WrapMode = 'ClampToEdge,
        min-filter : FilterMode = 'Linear,
        mag-filter : FilterMode = 'Linear)

        texture-view := TextureView texture
        sampler :=
            gpu.get-internal-sampler f"plonk.sampler-${wrap-mode}-${min-filter}"
                fn ()
                    Sampler wrap-mode min-filter

        super-type.__typecall cls
            texture = texture
            bind-group = (create-bind-group texture-view sampler)
    case (cls, texture : Texture, bind-group : BindGroup)
        super-type.__typecall *...

    fn get-key (self)
        'get-id self.bind-group

    fn get-texture (self)
        self.texture

    let BindGroupLayout = `(get-bind-group-layout)

do
    let TextureBinding
    local-scope;
