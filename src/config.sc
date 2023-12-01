using import Array C.stdlib Option print String struct

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

@@ from-environment "BOTTLE_WGPU_LOG_LEVEL" wgpu.LogLevel.Warn
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
            root : (Option String)
            save-directory : (Option String)
    gpu :
        struct GPUConfig
            # FIXME: validate this
            msaa-samples = 1:u8
            power-preference : wgpu.PowerPreference = 'HighPerformance
            present-mode : wgpu.PresentMode = 'Fifo
            wgpu-low-level-backend : wgpu.InstanceBackend = 'Primary
            wgpu-log-level : wgpu.LogLevel = 'Warn
    enabled-modules :
        struct BottleEnabledModules plain
            plonk = true
            imgui = true

    fn disable-module-by-name (self name)
        inline may-disable-module (k)
            if (name == (k as zarray))
                (getattr self.enabled-modules k) = false

        may-disable-module 'plonk
        may-disable-module 'imgui

    fn apply-env-overrides (self)
        if (not self.ignore-environment-variables?)
            self.gpu.wgpu-low-level-backend = (env-wgpu-backend)
            self.gpu.wgpu-log-level         = (env-wgpu-log-level)

            disabled-modules := (env-disabled-modules)
            for module in (env-disabled-modules)
                'disable-module-by-name self module

global config : BottleConfig

spice inline? (f)
    `[(sc_template_is_inline (sc_closure_get_template (f as Closure)))]
run-stage;

inline if-module-enabled (name)
    inline (f)
        inline (...)
            enabled? := getattr config.enabled-modules name
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

@@ memo
inline cfg-accessor (field)
    name := static-eval (('unique Symbol "BottleConfigAccessor") as string)
    type (_ name)
        inline __typeattr (cls attr)
            getattr
                getattr config field
                attr

do
    let if-module-enabled config cfg-accessor
    locals;
