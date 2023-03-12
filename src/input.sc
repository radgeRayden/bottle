# DESIGN NOTES
# ================================================================================

 - Virtual "player"(?) object can listen to events from one or more controllers, keyboard and mouse.
 - This object contains bindings attached to actions/commands, and can be queried for input state.
 - Any number of bindings can map to the same underlying input events, and responded to contextually.
 - Two types of bindings: axis and buttons.
 - Axis can be bound to controller analogs, mouse movement or mapped to digital inputs with configurable acceleration.
 - Buttons can be mapped to controller buttons and keyboard keys. Additionally, axis inputs
   can be mapped to a button via a configurable threshold.

# ================================================================================

using import Array
using import enum
using import Map
using import Set
using import String
using import struct

import .enums
import sdl

using enums

# ================================================================================
enum ButtonInput plain
    Pressed
    Released

    inline __tobool (self)
        self == this-type.Pressed

let ButtonCommand = (pointer (function void))
let AxisCommand = (pointer (function void f32))

struct ButtonAction plain
    command : ButtonCommand
    button-input : ButtonInput

struct AxisAction plain
    command : AxisCommand

enum ButtonBinding
    Button : ControllerButton
    Key    : KeyboardKey
    Axis   : ControllerAxis f32

enum AxisBinding
    Button : ControllerButton f32
    Key    : KeyboardKey f32
    Axis   : ControllerAxis

# for lookup
enum InputType
    Button : ControllerButton
    Key    : KeyboardKey
    Axis   : ControllerAxis

fn normalize-axis (value)
    ? (value >= 0) (value / 32767) ((value as i32) / 32768)

fn get-controller

# NOTE: currently lacking multi controller support.
struct InputLayer
    virtual-buttons : (Map String (Map InputType ButtonBinding))
    virtual-axis    : (Map String (Map InputType AxisBinding))
    button-actions  : (Map String ButtonAction)
    axis-actions    : (Map String AxisAction)
    input-lookup    : (Map InputType String)

    fn define-button (self name)
        'set self.virtual-buttons (String name) ((Map InputType ButtonBinding))

    fn define-axis (self name)
        'set self.virtual-axis (String name) ((Map InputType AxisBinding))

    fn... map-action (self, button-name, command : ButtonCommand, input : ButtonInput)
        button-name := (String button-name)

        if ('in? self.virtual-buttons button-name)
            'set self.button-actions button-name (ButtonAction command input)
        else
            report "tried to map an action to an unknown virtual button"
    case (self, axis-name, command : AxisCommand)
        axis-name := (String axis-name)

        if ('in? self.virtual-axis axis-name)
            'set self.axis-actions axis-name (AxisAction command)
        else
            report "tried to map an action to an unknown virtual axis"

    inline _bind-to-button (self button-name input-kind input threshold)
        let lookupT bindingT = (getattr InputType input-kind) (getattr ButtonBinding input-kind)

        if (not ('in? self.input-lookup (lookupT input)))
            try
                button-name := (String button-name)
                bindings := ('get self.virtual-buttons button-name)
                static-if (none? threshold)
                    'set bindings (lookupT input) (bindingT input)
                else
                    'set bindings (lookupT input) (bindingT input threshold)
                'set self.input-lookup (lookupT input) button-name
            else
                report "tried to bind an input to an unknown virtual button"
                ;
        else
            report "input already bound to an action"
            ;

    fn... bind-to-button (self, button-name, controller-button : ControllerButton)
        _bind-to-button self button-name 'Button controller-button
    case (self, button-name, keyboard-key : KeyboardKey)
        _bind-to-button self button-name 'Key keyboard-key
    case (self, button-name, controller-axis : ControllerAxis, threshold : f32)
        _bind-to-button self button-name 'Axis controller-axis threshold

    unlet _bind-to-button

    inline _bind-to-axis (self axis-name input-kind input value)
        let lookupT bindingT = (getattr InputType input-kind) (getattr AxisBinding input-kind)

        if (not ('in? self.input-lookup (lookupT input)))
            try
                axis-name := (String axis-name)
                bindings := ('get self.virtual-axis axis-name)
                static-if (none? value)
                    'set bindings (lookupT input) (bindingT input)
                else
                    'set bindings (lookupT input) (bindingT input value)
                'set self.input-lookup (lookupT input) axis-name
            else
                report "tried to bind an input to an unknown virtual axis"
                ;
        else
            report "input already bound to an action"
            ;

    fn... bind-to-axis (self, axis-name, controller-button : ControllerButton, value : f32)
        _bind-to-axis self axis-name 'Button controller-button value
    case (self, axis-name, keyboard-key : KeyboardKey, value : f32)
        _bind-to-axis self axis-name 'Key keyboard-key value
    case (self, axis-name, controller-axis : ControllerAxis)
        _bind-to-axis self axis-name 'Axis controller-axis

    unlet _bind-to-axis

    fn button-down? (self button-name)
        try
            button-name := (String button-name)
            bindings := ('get self.virtual-buttons button-name)
            fold (down? = false) for binding in bindings
                vvv bind result
                dispatch binding
                case Button (button)
                    bool (sdl.GameControllerGetButton (get-controller 0) (button as sdl.GameControllerButton))
                case Key (key)
                    kbstate := (sdl.GetKeyboardState null)
                    bool (kbstate @ (sdl.GetScancodeFromKey (key as sdl.KeyCode)))
                case Axis (axis threshold)
                    axis :=
                        normalize-axis
                            sdl.GameControllerGetAxis (get-controller 0) (axis as sdl.GameControllerAxis)

                    let result =
                        if (threshold > 0)
                            axis >= threshold
                        else
                            axis <= threshold

                    down? or result
                default
                    assert false
                    unreachable;

                down? or result
        else
            report "tried to query state of unknown virtual button"
            false

    fn get-axis (self axis-name)
        try
            axis-name := (String axis-name)
            bindings := ('get self.virtual-axis axis-name)

            vvv bind result
            fold (result = 0.0) for binding in bindings
                vvv bind value
                dispatch binding
                case Axis (axis)
                    axis :=
                        normalize-axis
                            sdl.GameControllerGetAxis (get-controller 0) (axis as sdl.GameControllerAxis)
                case Button (button value)
                    down? :=
                        bool (sdl.GameControllerGetButton (get-controller 0) (button as sdl.GameControllerButton))
                    ? down? value 0.0
                case Key (key value)
                    kbstate := (sdl.GetKeyboardState null)
                    down? :=
                        bool (kbstate @ (sdl.GetScancodeFromKey (key as sdl.KeyCode)))
                    ? down? value 0.0
                default
                    assert false
                    unreachable;

                result + value

            ? (result >= 0.0) (max result 1.0) (min result -1.0)

        else
            report "tried to query state of unknown virtual axis"
            0.0

    fn trigger-input (self input ...)
        let lookup-key =
            static-match (typeof input)
            case ControllerButton
                InputType.Button input
            case KeyboardKey
                InputType.Key input
            case ControllerAxis
                InputType.Axis input
            default
                unreachable;

        let binding-name =
            try ('get self.input-lookup lookup-key)
            else (return)

        # virtual buttons
        try
            action := ('get self.button-actions binding-name)
            binding := ('get ('get self.virtual-buttons binding-name) lookup-key)
            dispatch binding
            case Axis (axis threshold)
                value := (va-option value ... 0.0)
                if action.button-input
                    if (value >= threshold)
                        action.command;
                else
                    if (value < threshold)
                        action.command;
            default
                pressed? := (va-option pressed? ... false)
                if (action.button-input == pressed?)
                    action.command;
        else ()

        # virtual axis
        try
            action := ('get self.axis-actions binding-name)
            binding := ('get ('get self.virtual-axis binding-name) lookup-key)
            dispatch binding
            case Button (button value)
                pressed? := (va-option pressed? ... false)
                if pressed?
                    action.command value
                else
                    action.command 0.0
            case Key (key value)
                pressed? := (va-option pressed? ... false)
                if pressed?
                    action.command value
                else
                    action.command 0.0
            default
                value := (va-option value ... 0.0)
                if (none? value)
                    return;

                action.command value

        else ()

struct InputState
    controllers : (Map i32 (mutable@ sdl.GameController))
    active-layers : (Array InputLayer)

global istate : InputState

fn register-layer (layer)
    'append istate.active-layers layer

fn get-controller (id)
    'getdefault istate.controllers id null

# HOOKS
# ================================================================================
let cb = (import .sysevents.callbacks)

@@ 'on cb.controller-added
fn (id)
    'set istate.controllers id (sdl.GameControllerOpen id)

@@ 'on cb.controller-removed
fn (id)
    sdl.GameControllerClose ('getdefault istate.controllers id (nullof (mutable@ sdl.GameController)))
    'discard istate.controllers id

inline dispatch-input (input pressed? value)

@@ 'on cb.controller-button-pressed
fn (idx button)
    for layer in istate.active-layers
        'trigger-input layer button (pressed? = true)

@@ 'on cb.controller-button-released
fn (idx button)
    for layer in istate.active-layers
        'trigger-input layer button (pressed? = false)

@@ 'on cb.key-pressed
fn (key)
    for layer in istate.active-layers
        'trigger-input layer key (pressed? = true)

@@ 'on cb.key-released
fn (key)
    for layer in istate.active-layers
        'trigger-input layer key (pressed? = false)

@@ 'on cb.controller-axis-moved
fn (idx axis value)
    for layer in istate.active-layers
        'trigger-input layer axis (value = (normalize-axis value))

do
    let InputLayer ButtonInput
    let register-layer
    locals;
