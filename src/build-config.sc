using import enum

enum LogLevel plain
    Debug
    Info
    Warning
    Fatal

typedef BottleBuildConfig
    MinimumLogLevel := LogLevel.Debug

do
    let BottleBuildConfig LogLevel
    local-scope;
