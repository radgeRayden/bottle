VERSION :=
    label get-version
        try
            using import radl.IO.FileStream
            version-file := FileStream (module-dir .. "/../BOTTLE_VERSION") FileMode.Read
            merge get-version ('read-all-string version-file)
        else ()

        using import radl.version-string
        git-version;

VERSION as:= string
run-stage;

do
    fn get-version ()
        VERSION

    local-scope;
