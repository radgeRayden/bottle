import platform
import os
from doit.tools import LongRunning

DOIT_CONFIG = {'default_tasks': ['build_demos']}
FILTER_ANSI = os.getenv("DOIT_FILTER_ANSI")

def cmd(cmd):
    if platform.system() == "Linux":
        prefix = "MANGOHUD=1 MANGOHUD_CONFIG=horizontal,gpu_name,gpu_power,present_mode,resolution,vram,vulkan_driver,position=bottom-left bash -c"
    else:
        prefix = "bash -c"
    return f"{prefix} \"{cmd}\""

def task_eo():
    return {
        'actions': [cmd("wget -O eo https://hg.sr.ht/~duangle/majoreo/raw/eo?rev=tip"), cmd("chmod +x eo")],
        'targets': ["eo"],
        'uptodate': [True]
    }

def is_eo_initialized():
    return os.path.isdir(".eo/")

def task_eo_init():
    return {
        'actions': [cmd("./eo init && touch ./.eo/initialized")],
        'targets': [".eo/initialized"],
        'uptodate': [is_eo_initialized],
    }

bootstrap = ".eo/installed/bootstrap"
def task_bootstrap():
    return {
        'verbosity': 2,
        'actions': [cmd("yes | ./eo sync && ./eo install -y bootstrap")],
        'targets': [bootstrap],
        'uptodate': [False],
    }

def task_force_bootstrap():
    return {
        'verbosity': 2,
        'actions': [cmd("rm -rf .eo lib include"), cmd("./eo init"), cmd("./eo install -y bootstrap")],
        'uptodate': [False],
        'file_dep': ["eo"],
    }

def demo_cmd(name):
    return cmd(f"RUST_BACKTRACE=1 scopes -e run.sc {name}")

demos = []
with open('demos/demo-list.txt', 'r') as file:
    demo_list = file.read().split('\n')
    demos = [demo for demo in demo_list if demo]

if platform.system() == "Linux":
    with open('demos/demo-list-linux.txt', 'r') as file:
        demo_list = file.read().split('\n')
        linux_demos = [demo for demo in demo_list if demo]
        demos = demos + linux_demos

def demo_exe_name(name):
    sanitized_name = name.replace(".", "_")
    if platform.system() == "Windows":
        ext = ".exe"
    else:
        ext = ""
    return f"demo_{sanitized_name}{ext}"

def task_demos ():
    for name in demos:
        yield {
            'basename': f"demo.{name}",
            # 'actions': [LongRunning(demo_cmd(name))],
            'actions': 
                [LongRunning(cmd(f"./demos/build.sh {name}")),
                 LongRunning(cmd(f"pushd ./demos/bottle-demos && ./{demo_exe_name(name)}"))],
            'file_dep': [bootstrap],
            'uptodate': [False]
        }

def task_build_demos():
    return {
        'verbosity': 2,
        'actions': [cmd("./demos/build.sh")],
        'targets': ["demos/bottle-demos.zip"],
        'file_dep': [bootstrap],
        'uptodate': [False]
    }
