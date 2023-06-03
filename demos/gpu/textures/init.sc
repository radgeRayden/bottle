bottle := __env.bottle
using bottle.gpu.types
using bottle.enums

@@ 'on bottle.load
fn ()
    print ((RendererBackendInfo) . RendererString)

    try
        vert := ShaderModule (import .vert) ShaderLanguage.SPIRV ShaderStage.Vertex
        frag := ShaderModule (import .frag) ShaderLanguage.SPIRV ShaderStage.Fragment
        ;
    else ()

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
