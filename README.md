Every game programmer eventually has the urge to create their own game engine. This is mine. It's written in Scopes, an experimental programming language. Learn more about it here: http://scopes.rocks/

Basic goals (for a first version):
- Good enough for sprite based games
- can be combined with game code in a standalone executable (+ supporting shared libraries)

Examples can be found in the `demos` folder.

### Installation
Once cloned, you can install dependencies like so:

``` sh
cd bottle
wget https://hg.sr.ht/~duangle/majoreo/raw/eo?rev=tip -O ./eo
chmod +x eo
./eo init
./eo install -y bootstrap
```

Currently Linux and Windows (MSYS2 MinGW) are supported. You may need to install the following programs prior to running the bootstrap recipe:
- git
- cmake
- gcc
- make
- rustc
- python3

If it seems like you have all dependencies and it still fails to install, please open an issue.

After bootstrapping, you can then start a new project:

``` sh
mkdir my_project
cd my_project
python3 ../bottle/install.py
```

Happy coding!

