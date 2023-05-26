import .wgpu

type TextureView < Struct :: (storageof wgpu.TextureView)
    inline... __typecall (cls, value : (storageof this-type))
        bitcast value cls

do
    let TextureView
    local-scope;
