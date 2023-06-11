using import FunctionChain

do
    using (import ".sysevents.callbacks")

    fnchain configure
    fnchain load
    fnchain update
    fnchain fixed-update
    fnchain begin-frame
    fnchain render
    fnchain end-frame

    local-scope;
