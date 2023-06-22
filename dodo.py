from doit.tools import LongRunning

def task_eo():
    return {
        'actions': ["wget -O eo https://hg.sr.ht/~duangle/majoreo/raw/eo?rev=tip", "chmod +x eo"],
        'targets': ["eo"],
        'uptodate': [True]
    }

bootstrap = ".eo/installed/bootstrap"
def task_bootstrap():
    return {
        'actions': ["./eo install -y bootstrap"],
        'file_dep': ["eo"],
        'targets': [bootstrap]
    }

def task_force_bootstrap():
    return {
        'actions': ["rm -r .eo lib include", "./eo init", "./eo install -y bootstrap"],
        'uptodate': [False],
        'file_dep': ["eo"],
    }

def demo_cmd(name):
    return f"scopes -e run.sc {name}"

def task_demo_snake():
    return {
        'actions': [LongRunning(demo_cmd("snake"))],
        'file_dep': [bootstrap],
        'uptodate': [False]
    }
