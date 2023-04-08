#!/usr/bin/env python3

import os
import shutil
import urllib.request

bottle_src = os.path.dirname(os.path.realpath(__file__))
install_dir = os.getcwd()
bottle_dst = f"{install_dir}/lib/scopes/packages/bottle"

shutil.copytree(f"{bottle_src}/recipes", f"{install_dir}/recipes")

os.makedirs(bottle_dst, 0o777)
shutil.copytree(f"{bottle_src}/src", f"{bottle_dst}/src")
shutil.copyfile(f"{bottle_src}/init.sc", f"{bottle_dst}/init.sc")
shutil.copyfile(f"{bottle_src}/LICENSE", f"{bottle_dst}/LICENSE")
shutil.copyfile(f"{bottle_src}/README.md", f"{bottle_dst}/README.md")

eo_url = "https://hg.sr.ht/~duangle/majoreo/raw/eo?rev=tip"
urllib.request.urlretrieve(eo_url, "eo")
os.system("python3 eo init && python3 eo install -y wgpu-native-release sdl2 stb fontdue-native")

open("__env.sc", 'a').close()
