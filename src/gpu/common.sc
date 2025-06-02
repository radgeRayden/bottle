using import glm Map Option print String struct ..context ..exceptions 
ctx := context-accessor 'gpu

import .wgpu .types ..logger

spice wrap-nullable-object (cls object)
    spice-quote
        if (object == null)
            print "OBJECT CREATION FAILED:" [((tostring ('anchor object)) as string)]
            raise GPUError.ObjectCreationFailed
        else
            imply object cls

spice capture-validation-error (createf ...)
    anchor := 'anchor ...
    spice-quote
        wgpu.DevicePushErrorScope ctx.device 'Validation
        result := createf ...
        local failure? : bool
        wgpu.DevicePopErrorScope ctx.device
            typeinit
                mode = 'AllowProcessEvents
                callback =
                    fn (status error-type message ud1 ud2)
                        if (status == 'Success and error-type == 'Validation)
                            logger.write-warning@ [anchor] (imply message String)
                            (@ (ud1 as (mutable@ bool))) = true
                userdata1 = &failure? as voidstar

        wgpu.InstanceProcessEvents ctx.instance
        if failure?
            raise GPUError.ObjectCreationFailed
        else result

do
    let wrap-nullable-object capture-validation-error
    local-scope;
