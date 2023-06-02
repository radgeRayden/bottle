bottle := __env.bottle
using bottle.gpu.types
using bottle.enums

@@ 'on bottle.load
fn ()
    try
        vert := ShaderModule (import .vert) ShaderLanguage.SPIRV ShaderStage.Vertex
        frag := ShaderModule (import .frag) ShaderLanguage.SPIRV ShaderStage.Fragment
        ;
    else ()
    info := (bottle.gpu.info.get-backend-info)
    info := info._wgpu-adapter-properties
    using import String
    print (String info.name)
    print (String info.vendorName)

bottle.run;
