using import glm
using import Map
using import Option
using import print
using import String
using import struct
using import ..context ..exceptions

ctx := context-accessor 'gpu

import .wgpu
import .types

spice wrap-nullable-object (cls object)
    spice-quote
        if (object == null)
            print "OBJECT CREATION FAILED:" [((tostring ('anchor object)) as string)]
            raise GPUError.ObjectCreationFailed
        else
            imply object cls

do
    let wrap-nullable-object
    locals;
