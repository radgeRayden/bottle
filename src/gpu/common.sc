using import glm Map Option print String struct ..context ..exceptions 
ctx := context-accessor 'gpu

import .wgpu .types ..logger

inline wrap-in-error-scope (anchor error-filter exception-kind f args...)
    wgpu.DevicePushErrorScope ctx.device error-filter
    result := f args...
    local failure? : bool
    wgpu.DevicePopErrorScope ctx.device
        typeinit
            mode = 'AllowProcessEvents
            callback =
                fn (status error-type message ud1 ud2)
                    if (status == 'Success and error-type == error-filter)
                        logger.write-warning@ anchor (imply message String)
                        (@ (ud1 as (mutable@ bool))) = true
            userdata1 = &failure? as voidstar

    # TODO: consider if we always want to process immediately
    wgpu.InstanceProcessEvents ctx.instance
    if failure?
        raise GPUError.ObjectCreationFailed
    else result

inline make-error-scope-spice (error-filter exception-kind)
    spice (f)
        # TODO: noop if we have error reporting disabled
        anchor := 'anchor f
        spice-quote
            inline (...)
                wrap-in-error-scope [anchor] [error-filter] [exception-kind] f ...

do
    capture-validation-error := make-error-scope-spice 'Validation GPUError.InvalidInput
    wrap-object-creation := make-error-scope-spice 'Validation GPUError.ObjectCreationFailed

    local-scope;
