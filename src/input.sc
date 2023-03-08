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
using import String
using import struct

import .enums
import sdl

using enums

# ================================================================================

enum BindingType
    Button : ControllerButton
    Key    : KeyboardKey
    Axis   : ControllerAxis

struct InputBinding
    type : BindingType
    pressed? : bool

    inline __typecall (cls input pressed?)
        let binding-type =
            static-match (typeof input)
            case ControllerButton
                BindingType.Button input
            case KeyboardKey
                BindingType.Key input
            case ControllerAxis
                BindingType.Axis input
            default
                static-assert false

        super-type.__typecall cls
            type = binding-type
            pressed? = pressed?

    inline __hash (self)
        hash self.type self.pressed?

    inline __== (lhsT rhsT)
        static-if (lhsT == rhsT)
            inline (lhs rhs)
                and
                    lhs.type == rhs.type
                    lhs.pressed? == rhs.pressed?

let CommandFunction = (pointer (function void))

struct InputLayer
    mappings : (Map String CommandFunction)
    input-bindings : (Map InputBinding String)
    binding-lookup : (Map String (Array InputBinding))

    fn... create-mapping (self, mapping, command : CommandFunction)
        'set self.mappings (copy mapping) command
        'set self.binding-lookup mapping ((Array InputBinding))

    fn bind-input (self mapping input pressed?)
        # if the mapping doesn't exist, nothing happens.
        try
            'append ('get self.binding-lookup mapping)
                InputBinding input pressed?
            'set self.input-bindings
                InputBinding input pressed?
                mapping
        else
            ;


    fn clear-mapping (self mapping)
        # if the mapping doesn't exist, nothing happens.
        try
            bindings := ('get self.binding-lookup mapping)
            for b in bindings
                'discard self.input-bindings b

            'clear bindings
        else
            ;

    fn query-state (self mapping)
        # TODO

    fn trigger-input (self input pressed?)
        try
            mapping-name :=
                'get self.input-bindings
                    InputBinding input pressed?
            command := ('get self.mappings mapping-name)
            command;
        else
            ;

struct InputState
    controllers : (Map i32 (mutable@ sdl.GameController))
    active-layers : (Array InputLayer)

global istate : InputState

fn register-layer (layer)
    'append istate.active-layers layer

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

inline dispatch-input (input pressed?)
    for layer in istate.active-layers
        'trigger-input layer input pressed?

@@ 'on cb.controller-button-pressed
fn (idx button)
    dispatch-input button true

@@ 'on cb.controller-button-released
fn (idx button)
    dispatch-input button false

@@ 'on cb.key-pressed
fn (key)
    dispatch-input key true

@@ 'on cb.key-released
fn (key)
    dispatch-input key false

do
    let BindingType InputBinding InputLayer
    let register-layer
    locals;
