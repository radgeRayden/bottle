using import Array Map radl.strfmt slice String
import C.bindings
from C.bindings.extern let popen pclose feof fread

fn read-from-process (cmd)
    handle := popen (.. "bash -c '" cmd "' 2> /dev/null") "r"
    local result : String
    while (not ((feof handle) as bool))
        local c : i8
        fread &c 1 1 handle
        if (c != 0)
            'append result c

    if ((pclose handle) == 0)
        return (deref result)
    S""

cflags :=
    label trim-whitespace
        output := read-from-process "pkg-config --cflags libevdev"
        for i in (rrange (countof output))
            c := output @ i
            if (c != char" " and c != "\n")
                merge trim-whitespace ((trim (lslice output (i + 1))) as string)
        output as string
run-stage;

let evdev =
    include
        """"#include "libevdev/libevdev.h"
            #include "libevdev/libevdev-uinput.h"
            #include "linux/input-event-codes.h"
        options cflags

load-library "libevdev.so"
using import print

vvv bind evdev
do
    using evdev.extern filter "^libevdev_(.+)$"
    using evdev.const filter "^LIBEVDEV_.+$"
    using evdev.define filter "^BTN_.+$"
    using evdev.define filter "^ABS_.+$"
    using evdev.define filter "^REL_.+$"
    using evdev.define filter "^EV_.+$"
    using evdev.define filter "^SYN_.+$"
    using evdev.struct filter "^libevdev_(.+)$"
    using evdev.struct filter "^input_.+$"
    evdev := evdev.struct.libevdev
    local-scope;

run-stage;

using import print Option struct
import ...demo-common

import bottle

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "virtual controller spoofer"
    cfg.window.width = 100
    cfg.window.height = 100

inline... convert-button (button : bottle.enums.ControllerButton)
    switch button
    case 'A
        evdev.BTN_A
    case 'B
        evdev.BTN_B
    case 'X
        evdev.BTN_X
    case 'Y
        evdev.BTN_Y
    case 'Left
        evdev.BTN_DPAD_LEFT
    case 'Right
        evdev.BTN_DPAD_RIGHT
    case 'Up
        evdev.BTN_DPAD_UP
    case 'Down
        evdev.BTN_DPAD_DOWN
    case 'Back
        evdev.BTN_SELECT
    case 'Guide
        evdev.BTN_MODE
    case 'Start
        evdev.BTN_START
    case 'LeftBumper
        evdev.BTN_TL
    case 'RightBumper
        evdev.BTN_TR
    case 'LeftStick
        evdev.BTN_THUMBL
    case 'RightStick
        evdev.BTN_THUMBR
    default
        0

inline... convert-axis (axis : bottle.enums.ControllerAxis)
    switch axis
    case 'LeftX
        evdev.ABS_X
    case 'LeftY
        evdev.ABS_Y
    case 'RightX
        evdev.ABS_RX
    case 'RightY
        evdev.ABS_RY
    case 'LeftTrigger
        evdev.ABS_HAT2Y
    case 'RightTrigger
        evdev.ABS_HAT2X
    default
        0

struct VirtualController
    dev : (mutable@ evdev.evdev)
    uidev : (mutable@ evdev.uinput)

    inline __typecall (cls name)
        device := (evdev.new)
        evdev.set_name device name
        evdev.set_id_vendor device 0x045e # Microsoft
        evdev.set_id_product device 0x028e # XBOX 360 Controller
        evdev.enable_event_type device evdev.EV_KEY
        va-map
            inline enable-button (btn)
                evdev.enable_event_code device evdev.EV_KEY (u32 btn) null
            va-map convert-button 'A 'B 'X 'Y 'Left 'Right 'Up 'Down 'Back 'Guide 'Start \
                'LeftBumper 'RightBumper 'LeftStick 'RightStick

        evdev.enable_event_type device evdev.EV_ABS

        inline enable-axis (axis min max)
            local absinfo : evdev.input_absinfo
                resolution = 1000
                minimum = i32 (min * 32768)
                maximum = i32 (max * 32767)
            evdev.enable_event_code device evdev.EV_ABS (u32 (convert-axis axis)) &absinfo

        enable-axis 'LeftX -1 1
        enable-axis 'LeftY -1 1
        enable-axis 'RightX -1 1
        enable-axis 'RightY -1 1
        enable-axis 'LeftTrigger 0 1
        enable-axis 'RightTrigger 0 1

        local uidev : (mutable@ evdev.uinput)
        err := evdev.uinput_create_from_device device evdev.LIBEVDEV_UINPUT_OPEN_MANAGED &uidev
        if (err != 0)
            print err
            assert false

        super-type.__typecall cls
            dev = device
            uidev = uidev

    fn get-name (self)
        'from-rawstring String (evdev.get_name self.dev)

    fn get-devnode (self)
        devnode := evdev.uinput_get_devnode self.uidev
        if (devnode != null)
            'from-rawstring String devnode
        else
            S"unknown"

    fn... button-press (self, button : bottle.enums.ControllerButton)
        button := convert-button button
        if (button != 0)
            evdev.uinput_write_event self.uidev evdev.EV_KEY (u32 button) 1
            evdev.uinput_write_event self.uidev evdev.EV_SYN evdev.SYN_REPORT 0

    fn... button-release (self, button : bottle.enums.ControllerButton)
        button := convert-button button
        if (button != 0)
            evdev.uinput_write_event self.uidev evdev.EV_KEY (u32 button) 0
            evdev.uinput_write_event self.uidev evdev.EV_SYN evdev.SYN_REPORT 0

    fn... set-axis (self, axis : bottle.enums.ControllerAxis, value : f32)
        axis := convert-axis axis
        if (axis != 0)
            evdev.uinput_write_event self.uidev evdev.EV_ABS (u32 axis) (i32 (value * 32767))
            evdev.uinput_write_event self.uidev evdev.EV_SYN evdev.SYN_REPORT 0

    fn update (self)


    inline __drop (self)
        evdev.uinput_destroy self.uidev
        evdev.free self.dev

struct AppState
    controllers : (Map i32 VirtualController)
    current-controller : i32

global ctx : AppState

@@ 'on bottle.load
fn ()
    ctx = (AppState)

inline map-binding (key cb)
    switch key
    case 'Space
        cb 'A
    case 'z
        cb 'X
    case 'x
        cb 'B
    case 'c
        cb 'Y
    case 'Return
        cb 'Start
    case 'Backspace
        cb 'Back
    case 'Comma
        cb 'LeftBumper
    case 'Period
        cb 'RightBumper
    case 'Left
        cb 'Left
    case 'Right
        cb 'Right
    case 'Up
        cb 'Up
    case 'Down
        cb 'Down
    default
        ()

inline map-axis (key cb)
    shift? := bottle.keyboard.down? 'LShift
    CA := bottle.enums.ControllerAxis

    switch key
    case 'q
        cb 'LeftTrigger 1
    case 'e
        cb 'RightTrigger 1
    case 'w
        axis := shift? CA.RightY CA.LeftY
        cb axis -1
    case 'a
        axis := shift? CA.RightX CA.LeftX
        cb axis -1
    case 's
        axis := shift? CA.RightY CA.LeftY
        cb axis 1
    case 'd
        axis := shift? CA.RightX CA.LeftX
        cb axis 1
    default ()


using bottle.enums
@@ 'on bottle.key-pressed
fn (key)
    try ('get ctx.controllers ctx.current-controller)
    then (controller)
        map-binding key
            inline (button)
                'button-press controller button

        map-axis key
            inline (axis value)
                'set-axis controller axis value
    else ()

@@ 'on bottle.key-released
fn (key)
    inline toggle-controller (idx)
        if ('in? ctx.controllers idx)
            'discard ctx.controllers idx
        else
            'set ctx.controllers idx (VirtualController f"Xbox 360 Controller")

    switch key
    case KeyboardKey.1
        if (bottle.keyboard.down? 'LShift)
            ctx.current-controller = 1
        else
            toggle-controller 1
    case KeyboardKey.2
        if (bottle.keyboard.down? 'LShift)
            ctx.current-controller = 2
        else
            toggle-controller 2
    case KeyboardKey.3
        if (bottle.keyboard.down? 'LShift)
            ctx.current-controller = 3
        else
            toggle-controller 3
    case KeyboardKey.4
        if (bottle.keyboard.down? 'LShift)
            ctx.current-controller = 4
        else
            toggle-controller 4
    default
        ()

    try ('get ctx.controllers ctx.current-controller)
    then (controller)
        map-binding key
            inline (button)
                'button-release controller button
        map-axis key
            inline (axis value)
                'set-axis controller axis 0
    else ()

@@ 'on bottle.update
fn (dt)
    for k controller in ctx.controllers
        'update controller

@@ 'on bottle.quit
fn ()
    ctx = (AppState)

sugar-if main-module?
    bottle.run;
else
    fn main (argc argv)
        bottle.run;
        0
