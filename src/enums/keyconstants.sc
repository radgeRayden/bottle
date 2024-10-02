using import enum

sdl := import sdl3

enum KeyboardKey : u32
    Unknown = sdl.SDLK_UNKNOWN
    Return = sdl.SDLK_RETURN
    Escape = sdl.SDLK_ESCAPE
    Backspace = sdl.SDLK_BACKSPACE
    Tab = sdl.SDLK_TAB
    Space = sdl.SDLK_SPACE
    Apostrophe = sdl.SDLK_APOSTROPHE
    DoubleApostrophe = sdl.SDLK_DBLAPOSTROPHE
    Hash = sdl.SDLK_HASH
    LeftParen = sdl.SDLK_LEFTPAREN
    RightParen = sdl.SDLK_RIGHTPAREN
    Comma = sdl.SDLK_COMMA
    Period = sdl.SDLK_PERIOD

    Percent = sdl.SDLK_PERCENT
    Dollar = sdl.SDLK_DOLLAR
    Ampersand = sdl.SDLK_AMPERSAND
    Asterisk = sdl.SDLK_ASTERISK
    Exclaim = sdl.SDLK_EXCLAIM
    Minus = sdl.SDLK_MINUS
    Plus = sdl.SDLK_PLUS
    Slash = sdl.SDLK_SLASH

    tag (Symbol "0") Nothing sdl.SDLK_0
    tag (Symbol "1") Nothing sdl.SDLK_1
    tag (Symbol "2") Nothing sdl.SDLK_2
    tag (Symbol "3") Nothing sdl.SDLK_3
    tag (Symbol "4") Nothing sdl.SDLK_4
    tag (Symbol "5") Nothing sdl.SDLK_5
    tag (Symbol "6") Nothing sdl.SDLK_6
    tag (Symbol "7") Nothing sdl.SDLK_7
    tag (Symbol "8") Nothing sdl.SDLK_8
    tag (Symbol "9") Nothing sdl.SDLK_9

    Colon = sdl.SDLK_COLON
    Less = sdl.SDLK_LESS
    Equals = sdl.SDLK_EQUALS
    Greater = sdl.SDLK_GREATER
    Question = sdl.SDLK_QUESTION
    At = sdl.SDLK_AT

    Semicolon = sdl.SDLK_SEMICOLON
    LeftBracket = sdl.SDLK_LEFTBRACKET
    RightBracket = sdl.SDLK_RIGHTBRACKET
    Backslash = sdl.SDLK_BACKSLASH

    Caret = sdl.SDLK_CARET
    Underscore = sdl.SDLK_UNDERSCORE

    Backquote = sdl.SDLK_GRAVE

    a = sdl.SDLK_A
    b = sdl.SDLK_B
    c = sdl.SDLK_C
    d = sdl.SDLK_D
    e = sdl.SDLK_E
    f = sdl.SDLK_F
    g = sdl.SDLK_G
    h = sdl.SDLK_H
    i = sdl.SDLK_I
    j = sdl.SDLK_J
    k = sdl.SDLK_K
    l = sdl.SDLK_L
    m = sdl.SDLK_M
    n = sdl.SDLK_N
    o = sdl.SDLK_O
    p = sdl.SDLK_P
    q = sdl.SDLK_Q
    r = sdl.SDLK_R
    s = sdl.SDLK_S
    t = sdl.SDLK_T
    u = sdl.SDLK_U
    v = sdl.SDLK_V
    w = sdl.SDLK_W
    x = sdl.SDLK_X
    y = sdl.SDLK_Y
    z = sdl.SDLK_Z

    CapsLock = sdl.SDLK_CAPSLOCK

    F1 = sdl.SDLK_F1
    F2 = sdl.SDLK_F2
    F3 = sdl.SDLK_F3
    F4 = sdl.SDLK_F4
    F5 = sdl.SDLK_F5
    F6 = sdl.SDLK_F6
    F7 = sdl.SDLK_F7
    F8 = sdl.SDLK_F8
    F9 = sdl.SDLK_F9
    F10 = sdl.SDLK_F10
    F11 = sdl.SDLK_F11
    F12 = sdl.SDLK_F12

    PrintScreen = sdl.SDLK_PRINTSCREEN
    ScrollLock = sdl.SDLK_SCROLLLOCK
    Pause = sdl.SDLK_PAUSE
    Insert = sdl.SDLK_INSERT
    Home = sdl.SDLK_HOME
    PageUp = sdl.SDLK_PAGEUP
    Delete = sdl.SDLK_DELETE
    End = sdl.SDLK_END
    PageDown = sdl.SDLK_PAGEDOWN
    Right = sdl.SDLK_RIGHT
    Left = sdl.SDLK_LEFT
    Down = sdl.SDLK_DOWN
    Up = sdl.SDLK_UP

    NumLock = sdl.SDLK_NUMLOCKCLEAR
    KpDivide = sdl.SDLK_KP_DIVIDE
    KpMultiply = sdl.SDLK_KP_MULTIPLY
    KpMinus = sdl.SDLK_KP_MINUS
    KpPlus = sdl.SDLK_KP_PLUS
    KpEnter = sdl.SDLK_KP_ENTER
    Kp1 = sdl.SDLK_KP_1
    Kp2 = sdl.SDLK_KP_2
    Kp3 = sdl.SDLK_KP_3
    Kp4 = sdl.SDLK_KP_4
    Kp5 = sdl.SDLK_KP_5
    Kp6 = sdl.SDLK_KP_6
    Kp7 = sdl.SDLK_KP_7
    Kp8 = sdl.SDLK_KP_8
    Kp9 = sdl.SDLK_KP_9
    Kp0 = sdl.SDLK_KP_0
    KpPeriod = sdl.SDLK_KP_PERIOD
    KpComma = sdl.SDLK_KP_COMMA
    KpEqualsAs400 = sdl.SDLK_KP_EQUALSAS400
    KpEquals = sdl.SDLK_KP_EQUALS

    Application = sdl.SDLK_APPLICATION
    Power = sdl.SDLK_POWER
    F13 = sdl.SDLK_F13
    F14 = sdl.SDLK_F14
    F15 = sdl.SDLK_F15
    F16 = sdl.SDLK_F16
    F17 = sdl.SDLK_F17
    F18 = sdl.SDLK_F18
    F19 = sdl.SDLK_F19
    F20 = sdl.SDLK_F20
    F21 = sdl.SDLK_F21
    F22 = sdl.SDLK_F22
    F23 = sdl.SDLK_F23
    F24 = sdl.SDLK_F24
    Execute = sdl.SDLK_EXECUTE
    Help = sdl.SDLK_HELP
    Menu = sdl.SDLK_MENU
    Select = sdl.SDLK_SELECT
    Stop = sdl.SDLK_STOP
    Again = sdl.SDLK_AGAIN
    Undo = sdl.SDLK_UNDO
    Cut = sdl.SDLK_CUT
    Copy = sdl.SDLK_COPY
    Paste = sdl.SDLK_PASTE
    Find = sdl.SDLK_FIND
    Mute = sdl.SDLK_MUTE
    VolumeUp = sdl.SDLK_VOLUMEUP
    VolumeDown = sdl.SDLK_VOLUMEDOWN

    AltErase = sdl.SDLK_ALTERASE
    SysReq = sdl.SDLK_SYSREQ
    Cancel = sdl.SDLK_CANCEL
    Clear = sdl.SDLK_CLEAR
    Prior = sdl.SDLK_PRIOR
    Return2 = sdl.SDLK_RETURN2
    Separator = sdl.SDLK_SEPARATOR
    Out = sdl.SDLK_OUT
    Oper = sdl.SDLK_OPER
    ClearAgain = sdl.SDLK_CLEARAGAIN
    CrSel = sdl.SDLK_CRSEL
    ExSel = sdl.SDLK_EXSEL

    Kp00 = sdl.SDLK_KP_00
    Kp000 = sdl.SDLK_KP_000
    ThousandsSeparator = sdl.SDLK_THOUSANDSSEPARATOR
    DecimalSeparator = sdl.SDLK_DECIMALSEPARATOR
    CurrencyUnit = sdl.SDLK_CURRENCYUNIT
    CurrencySubUnit = sdl.SDLK_CURRENCYSUBUNIT
    KpLeftParen = sdl.SDLK_KP_LEFTPAREN
    KpRightParen = sdl.SDLK_KP_RIGHTPAREN
    KpLeftBrace = sdl.SDLK_KP_LEFTBRACE
    KpRightBrace = sdl.SDLK_KP_RIGHTBRACE
    KpTab = sdl.SDLK_KP_TAB
    KpBackSpace = sdl.SDLK_KP_BACKSPACE
    KpA = sdl.SDLK_KP_A
    KpB = sdl.SDLK_KP_B
    KpC = sdl.SDLK_KP_C
    KpD = sdl.SDLK_KP_D
    KpE = sdl.SDLK_KP_E
    KpF = sdl.SDLK_KP_F
    KpXor = sdl.SDLK_KP_XOR
    KpPower = sdl.SDLK_KP_POWER
    KpPercent = sdl.SDLK_KP_PERCENT
    KpLess = sdl.SDLK_KP_LESS
    KpGreater = sdl.SDLK_KP_GREATER
    KpAmpersand = sdl.SDLK_KP_AMPERSAND
    KpDoubleAmpersand = sdl.SDLK_KP_DBLAMPERSAND
    KpverticalBar = sdl.SDLK_KP_VERTICALBAR
    KpDoubleVerticalBar = sdl.SDLK_KP_DBLVERTICALBAR
    KpColon = sdl.SDLK_KP_COLON
    KpHash = sdl.SDLK_KP_HASH
    KpSpace = sdl.SDLK_KP_SPACE
    KpAt = sdl.SDLK_KP_AT
    KpExclaim = sdl.SDLK_KP_EXCLAM
    KpMemStore = sdl.SDLK_KP_MEMSTORE
    KpMemRecall = sdl.SDLK_KP_MEMRECALL
    KpMemClear = sdl.SDLK_KP_MEMCLEAR
    KpMemAdd = sdl.SDLK_KP_MEMADD
    KpMemSubtract = sdl.SDLK_KP_MEMSUBTRACT
    KpMemMultiply = sdl.SDLK_KP_MEMMULTIPLY
    KpMemDivide = sdl.SDLK_KP_MEMDIVIDE
    KpPlusMinus = sdl.SDLK_KP_PLUSMINUS
    KpClear = sdl.SDLK_KP_CLEAR
    KpClearEntry = sdl.SDLK_KP_CLEARENTRY
    KpBinary = sdl.SDLK_KP_BINARY
    KpOctal = sdl.SDLK_KP_OCTAL
    KpDecimal = sdl.SDLK_KP_DECIMAL
    KpHexadecimal = sdl.SDLK_KP_HEXADECIMAL

    LCtrl = sdl.SDLK_LCTRL
    LShift = sdl.SDLK_LSHIFT
    LAlt = sdl.SDLK_LALT
    LGui = sdl.SDLK_LGUI
    RCtrl = sdl.SDLK_RCTRL
    RShift = sdl.SDLK_RSHIFT
    RAlt = sdl.SDLK_RALT
    RGui = sdl.SDLK_RGUI

    Mode = sdl.SDLK_MODE

    MediaNextTrack = sdl.SDLK_MEDIA_NEXT_TRACK
    MediaPreviousTrack = sdl.SDLK_MEDIA_PREVIOUS_TRACK
    MediaStop = sdl.SDLK_MEDIA_STOP
    MediaPlay = sdl.SDLK_MEDIA_PLAY
    MediaRewind = sdl.SDLK_MEDIA_REWIND
    MediaFastForward = sdl.SDLK_MEDIA_FAST_FORWARD
    MediaSelect = sdl.SDLK_MEDIA_SELECT
    MediaEject = sdl.SDLK_MEDIA_EJECT
    AcSearch = sdl.SDLK_AC_SEARCH
    AcHome = sdl.SDLK_AC_HOME
    AcBack = sdl.SDLK_AC_BACK
    AcForward = sdl.SDLK_AC_FORWARD
    AcStop = sdl.SDLK_AC_STOP
    AcRefresh = sdl.SDLK_AC_REFRESH
    AcBookmarks = sdl.SDLK_AC_BOOKMARKS
    Sleep = sdl.SDLK_SLEEP

do
    let KeyboardKey
    locals;
