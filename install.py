#!/usr/bin/env python3

import os
import shutil
import urllib.request

bottle_src = os.path.dirname(os.path.realpath(__file__))
install_dir = os.getcwd()
bottle_dst = f"{install_dir}/lib/scopes/packages/bottle"

shutil.copytree(f"{bottle_src}/lib", f"{install_dir}/lib")
shutil.copytree(f"{bottle_src}/include", f"{install_dir}/include")

os.makedirs(bottle_dst, 0o777)
shutil.copytree(f"{bottle_src}/src", f"{bottle_dst}/src")
shutil.copyfile(f"{bottle_src}/init.sc", f"{bottle_dst}/init.sc")
shutil.copyfile(f"{bottle_src}/LICENSE", f"{bottle_dst}/LICENSE")
shutil.copyfile(f"{bottle_src}/README.md", f"{bottle_dst}/README.md")

open("__env.sc", 'a').close()
open(f"{bottle_dst}/BOTTLE_VERSION", "a").close()
os.chdir(bottle_src)
os.system(f"bash -c 'echo -n $(scopes -e -c \"sc_write (__env.bottle.get-version)\") > {bottle_dst}/BOTTLE_VERSION '")
