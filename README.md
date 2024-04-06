# InstancedImpostors

A simple test to optimize 3D rendering by turning distant objects into flat impostors.

I need this for my own game project (https://enormous-elk.itch.io/ancient-savo), as I'm developing on somewhat old hardware (Intel UHD graphics 620).
I'd guess the same approach could be useful when optimizing for mobile platforms, or on desktop modern graphics cards when in addition to a lot of very detailed objects in the foreground you also need lots and lots of objects in the background, like foliage and stuff.

Also, the basics of Level Of Detail using pre-generated simplified versions of models to be used when they are further away so that all the details won't be visible anyway.
One way to generate LOD version is to use decimate modifier in Blender. For this demo the LOD versions are generated using the Mesh Optimizer tool by Antz.

Even if you are not going to need LOD versions, I recommend taking a look at mesh optimizer tool https://antzgames.itch.io/libgdx-meshoptimizer , for optimization without reducing the quality might give a noticeable boost in performance.

Although this is for my own commercial game project, I wanted to release this piece of code to give back to the libGDX community.  

yours
 Erkka from Enormous Elk

PS. a quick note at 6th of April 2024; the current version source code is rather messy, and there are unused pieces of codes lingering around.
    That is because I've been experimenting with different optimization methods, and leaving the various versions of test code just in case I'd need them later.
    Also, many of the later additions are not properly commented, so it might be a pain to figure out how the code works.
    In case you wish to copy, modify and extend it for your own needs, feel free to do so. And just send me a message if there is something you struggle to figure out.


## Background

## Thanks and other info

Many of the core concepts based on https://github.com/antzGames/ModelInstancedRendering

and

https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/gles3/InstancedRenderingTest.java

Inspired by discussions on libGDX Discord around early March 2024
Also thanks to user with alias casper who originally introduced the concept of impostors to me. My approach is heavily based on what I learnt with discussions with them.

---

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.

## Gradle

This project uses [Gradle](http://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/lib`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
