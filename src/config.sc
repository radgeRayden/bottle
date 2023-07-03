using import Array
using import Option
using import print
using import String
using import struct

using import .enums
wgpu := import .gpu.wgpu

inline from-environment (name def)
    inline (handler)
        fn ()
            using import C.stdlib
            env-var := getenv name

            if (env-var == null)
                static-if ((typeof def) == Closure)
                    (def)
                else
                    def
            else
                env-var := 'from-rawstring String env-var
                try (handler env-var)
                else
                    using import radl.strfmt
                    print f"Unrecognized option for ${name}: ${env-var}"
                    def

from (import .helpers) let match-string-enum

@@ from-environment "BOTTLE_WGPU_INSTANCE_BACKEND" wgpu.InstanceBackend.Primary
fn env-wgpu-backend (value)
    match-string-enum wgpu.InstanceBackend value
        _ 'Vulkan 'DX12 'Metal 'GL 'DX11

@@ from-environment "BOTTLE_WGPU_LOG_LEVEL" wgpu.LogLevel.Error
fn env-wgpu-log-level (value)
    match-string-enum wgpu.LogLevel value
        _ 'Off 'Error 'Warn 'Info 'Debug 'Trace

@@ from-environment "BOTTLE_DISABLED_MODULES" (() -> ((Array String)))
fn env-disabled-modules (value)
    using import radl.String+
    split value S","

struct BottleConfig
    ignore-environment-variables? = false
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
            # FIXME: validate this
            msaa-samples = 1:u8
            wgpu-low-level-api = wgpu.InstanceBackend.Primary
            wgpu-log-level = wgpu.LogLevel.Error
    enabled-modules :
        struct BottleEnabledModules plain
            plonk = true
            imgui = true

    fn disable-module-by-name (self name)
        inline may-disable-module (k)
            if (name == (static-tostring k))
                (getattr self.enabled-modules k) = false

        may-disable-module 'plonk
        may-disable-module 'imgui

    fn apply-env-overrides (self)
        if (not self.ignore-environment-variables?)
            self.gpu.wgpu-low-level-api = (env-wgpu-backend)
            self.gpu.wgpu-log-level     = (env-wgpu-log-level)

            disabled-modules := (env-disabled-modules)
            for module in (env-disabled-modules)
                'disable-module-by-name self module

global istate-cfg : BottleConfig

spice inline? (f)
    `[(sc_template_is_inline (sc_closure_get_template (f as Closure)))]
run-stage;

inline if-module-enabled (name)
    inline (f)
        inline (...)
            enabled? := getattr istate-cfg.enabled-modules name
            let f =
                static-if (inline? f)
                    fn (...)
                        f ...
                else
                    f

            retT :=
                returnof
                    static-typify f (va-map typeof ...)
            if enabled?
                f ...
            else
                (retT)

do
    let istate-cfg if-module-enabled
    locals;
