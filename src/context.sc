using import Array glm hash Option String struct
import .gpu.wgpu .types

wgpu := gpu.wgpu

# HELPERS
# =======

spice collect-enum-fields (ET)
    using import Array radl.String+

    ET as:= type
    local args : (Array Symbol)
    for k v in ('symbols ET)
        if (not (starts-with? (String (k as string)) "_"))
            'append args k

    sc_argument_list_map_new (i32 (countof args))
        inline (i)
            arg := args @ i
            `arg

spice inline? (f)
    `[(sc_template_is_inline (sc_closure_get_template (f as Closure)))]

run-stage;

@@ memo
inline collect-enum-fields (ET)
    collect-enum-fields ET

inline match-string-enum (ET value)
    using import hash radl.String+ switcher print
    tolower := ASCII-tolower

    call
        switcher sw
            va-map
                inline (k)
                    case (static-eval (hash (tolower (k as string))))
                        imply k ET
                collect-enum-fields ET
            default
                raise;
        hash (tolower value)

# STARTUP CONFIGURATION
# =====================
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

@@ from-environment "BOTTLE_WGPU_INSTANCE_BACKEND" wgpu.InstanceBackend.Primary
fn env-wgpu-backend (value)
    match-string-enum wgpu.InstanceBackend value

@@ from-environment "BOTTLE_WGPU_LOG_LEVEL" wgpu.LogLevel.Warn
fn env-wgpu-log-level (value)
    match-string-enum wgpu.LogLevel value

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
            msaa? : bool
            power-preference : wgpu.PowerPreference = 'HighPerformance
            present-mode : wgpu.PresentMode = 'Fifo
            wgpu-low-level-backend : wgpu.InstanceBackend = 'Primary
            wgpu-log-level : wgpu.LogLevel = 'Warn
            enable-validation? : bool = true
    enabled-modules :
        struct BottleEnabledModules plain
            plonk = true
            imgui = true
    platform :
        struct PlatformConfig
            force-x11? : bool

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

struct WGPUAdapterInfo
    adapter : wgpu.Adapter
    properties : wgpu.AdapterProperties
    limits : wgpu.SupportedLimits
    supported-features : (Array wgpu.FeatureName)

struct BottleGPUState
    available-adapters      : (Array WGPUAdapterInfo)
    available-present-modes : (Array wgpu.PresentMode)
    limits : wgpu.Limits

    instance : wgpu.Instance
    surface  : wgpu.Surface
    adapter  : wgpu.Adapter
    device   : wgpu.Device
    queue    : wgpu.Queue

    surface-size : ivec2
    scaled-surface-size : ivec2
    cmd-encoder : (Option types.CommandEncoder)
    surface-texture : (Option types.Texture)
    surface-texture-view : (Option types.TextureView)
    msaa-resolve-source : (Option types.TextureView)
    outdated-surface? : bool

    clear-color = (vec4 0.017 0.017 0.017 1.0)
    msaa? : bool
    present-mode : wgpu.PresentMode

struct BottleContext
    config : BottleConfig
    gpu : BottleGPUState

global context : BottleContext

inline if-module-enabled (name)
    inline (f)
        inline (...)
            enabled? := getattr context.config.enabled-modules name
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
inline context-accessor (chain...)
    name := static-eval (('unique Symbol "BottleContextAccessor") as string)
    type (_ name)
        inline __typeattr (cls attr)
            getattr
                va-lfold context
                    inline (?? next computed)
                        getattr computed next
                    chain...
                attr

        inline __typecall (cls)
            va-lfold context
                inline (?? next computed)
                    getattr computed next
                chain...

do
    let if-module-enabled context-accessor
    local-scope;
