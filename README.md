# InstancedImpostors

A simple test to optimize 3D rendering by turning distant objects into flat impostors.
I need this for my own game project (https://enormous-elk.itch.io/ancient-savo), as I'm developing on somewhat old hardware (Intel HD graphics 4600).
I'd guess the same approach could be useful when optimizing for mobile platforms, or on desktop modern graphics cards when in addition to a lot of very detailed objects in the foreground you also need lots and lots of objects in the background, like foliage and stuff.

Also the basics of Level Of Detail, using pre-generated simplified versions of models to be used when they are further away so that all of the details wont be visible anyway.
For this demo I generated the LOD versions simply by using Decimate modifier in Blender. That was not the perfect solution, on some camera angles it is noticeable how lights and shadows change on the birch model due to too different geometry. But I decided to leave it as such for the demo, to given an impression of the possible caveats with the LOD approach.
At the moment of writing this Antz is working on a project to programmatically generate LOD versions of models in a libGDX application - interesting, very interesting!

Although this is for my own commercial game project, I wanted to release this piece of code to give back to the libGDX community.  

yours
 Erkka from Enormous Elk

## Background

## Thanks and other info

Many of the core concepts based on https://github.com/antzGames/ModelInstancedRendering
and https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/gles3/InstancedRenderingTest.java

Inspired by discussions on libGDX Discord around early March 2024
Also thanks to user with alias casper who originally introduced the concept of impostors to me. My approach is heavily based on what I learnt with discussions with them.

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

Project template included simple launchers and an `ApplicationAdapter` extension with GUI created using the [VisUI](https://github.com/kotcrab/vis-ui) library.

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
