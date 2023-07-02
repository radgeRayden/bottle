using import struct
using import Option
using import String

using import .enums
wgpu := import .gpu.wgpu

spice static-hash (str)
    `[(hash (str as string))]
run-stage;

inline env-var (name def)
    inline (f)
        fn ()
            using import C.stdlib
            env-var := getenv name
            if (env-var == null) def
            else
                try (f ('from-rawstring String env-var))
                else def

using import switcher
inline match-string-enum (enum-type value cases...)
    call
        switcher sw
            va-map
                inline (arg...)
                    let k v = (keyof arg...) arg...
                    case (static-hash v)
                        getattr enum-type k
                cases...
            default
                print "Unrecognized option:" value
                raise;
        value

@@ env-var "BOTTLE_FORCE_WGPU_BACKEND" wgpu.InstanceBackend.Primary
fn wgpu-backend (value)
    match-string-enum wgpu.InstanceBackend (hash value)
        _
            Vulkan = "vulkan"
            DX12 = "dx12"
            Metal = "metal"
            GL = "gl"
            DX11 = "dx11"

@@ env-var "BOTTLE_WGPU_LOG_LEVEL" wgpu.LogLevel.Error
fn wgpu-log-level (value)
    match-string-enum wgpu.LogLevel (hash value)
        _
            Off = "off"
            Error = "error"
            Warn = "warn"
            Info = "info"
            Debug = "debug"
            Trace = "trace"

struct BottleEnvironmentVariables plain
    wgpu-backend   : wgpu.InstanceBackendFlags
    wgpu-log-level : wgpu.LogLevel
    inline __typecall (cls)
        super-type.__typecall cls
            (wgpu-backend)
            (wgpu-log-level)

struct BottleConfig
    # TODO: use this as override
    # ignore-environment-variables? = false
    window :
        struct WindowConfig
            title = S"Game from Scratch Re:Birth"
            width  : (Option i32)
            height : (Option i32)
            relative-width  = 0.5
            relative-height = 0.5

            # initialization options
            fullscreen? = false
            hidden? = false
            borderless? = false
            resizable? = true
            minimized? = false
            maximized? = false
            always-on-top? = false
    time :
        struct TimeConfig
            use-delta-accumulator? : bool
            fixed-timestep : f64 = (1 / 60)
    filesystem :
        struct FilesystemConfig
            root = S"."
    gpu :
        struct GPUConfig
            power-preference = PowerPreference.HighPerformance

global istate-cfg : BottleConfig

do
    let istate-cfg BottleEnvironmentVariables
    locals;
