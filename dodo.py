from doit.tools import LongRunning

def cmd(cmd):
    return f"bash -c \"{cmd}\""

def task_eo():
    return {
        'actions': [cmd("wget -O eo https://hg.sr.ht/~duangle/majoreo/raw/eo?rev=tip"), cmd("chmod +x eo")],
        'targets': ["eo"],
        'uptodate': [True]
    }

bootstrap = ".eo/installed/bootstrap"
def task_bootstrap():
    return {
        'verbosity': 2,
        'actions': [cmd("./eo init; true"), cmd("./eo install -y bootstrap")],
        'file_dep': ["eo"],
        'targets': [bootstrap]
    }

def task_force_bootstrap():
    return {
        'verbosity': 2,
        'actions': [cmd("rm -r .eo lib include"), cmd("./eo init"), cmd("./eo install -y bootstrap")],
        'uptodate': [False],
        'file_dep': ["eo"],
    }

def demo_cmd(name):
    return cmd(f"scopes -e run.sc {name}")

demos = ["snake", "plonk.sprites", "gpu.hello-triangle", "gpu.buffers", "gpu.textures"]
def task_demos ():
    for name in demos:
        yield {
            'basename': f"demo.{name}",
            'actions': [LongRunning(demo_cmd(name))],
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