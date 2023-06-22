Every game programmer eventually has the urge to create their own game engine. This is mine. It's written in [Scopes](http://scopes.rocks/), an experimental programming language. Note you need a working install of Scopes in your path to use this framework.

Basic goals (for a first version):
- Good enough for sprite based games
- can be combined with game code in a standalone executable (+ supporting shared libraries)

Examples can be found in the `demos` folder.

### Building
Currently Linux and Windows (MSYS2 MinGW) are supported. If it seems like you have all dependencies and it still fails to install, please open an issue.

#### Linux

The exact package names for linux will depend on your distribution. 
You may need to install the following packages prior to running the bootstrap recipe:
- git
- cmake
- gcc
- make
- zip
- unzip
- rustc (might need to be installed [manually](https://www.rust-lang.org/tools/install) if your distro ships a version that is too old)
- clang (used to build some rust libraries)
- python3
- libsdl2-dev (for the X11 dependencies)

Here are examples for a couple common distributions; you can then translate to your favorite package manager / distribution.

Arch Linux:
```sh
pacman -S wget git base-devel rust python3 zip unzip sdl2 cmake clang
```

#### Windows (MinGW)

Install all needed dependencies inside a MSYS2 shell:
``` sh
pacman -S git mingw-w64-x86_64-python mingw-w64-x86_64-gcc zip unzip mingw-w64-x86_64-7zip mingw-w64-x86_64-make mingw-w64-x86_64-rust mingw-w64-x86_64-clang mingw-w64-x86_64-cmake
```

---

You should then clone bottle and bootstrap it:
``` sh
cd bottle
wget https://hg.sr.ht/~duangle/majoreo/raw/eo?rev=tip -O ./eo
chmod +x eo
./eo init
./eo install -y bootstrap
```

### Using Bottle

After bootstrapping, you can then start a new project:
``` sh
mkdir my_project
cd my_project
../bottle/install # initializes a new bottle project with all dependencies
scopes -e -c "import bottle; bottle.run;"
```

Happy coding!

