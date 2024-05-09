package com.enormouselk.sandbox.impostors.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.enormouselk.sandbox.impostors.ImpostorDemo;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {

        //first, we set the headers for libGDX default shaders, which the UI uses
        //for some reason, linux is happy to run without setting these, but mac complains if the glsl version is not explicitly defined
        //so here we go:
        ShaderProgram.prependFragmentCode = "#version 100\n";
        ShaderProgram.prependVertexCode = "#version 100\n";

        ImpostorDemo demo = new ImpostorDemo();
        Lwjgl3ApplicationConfiguration configuration = getDefaultConfiguration();

        //And then we define the OpenGL ES version 3.0, emulation based on OpenGL version 4.5
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,4,5);

        Lwjgl3Application app;
        try {
            app = new Lwjgl3Application(demo,configuration);
        } catch (Exception e) {
            if (e.getMessage().contains("create window"))
            {
                //apparently OpenGL 4.5 failed on the user machine
                //Revert to 3.2, maybe it could work, then?
                configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,2);
                try {
                    app = new Lwjgl3Application(demo,configuration);
                } catch (Exception e2) {
                    throw new RuntimeException(e2);
                }

            } else throw new RuntimeException(e);
        }

        return app;
        //return new Lwjgl3Application(new ImpostorDemo(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("InstancedImpostors Demo");

        //7.5.2024 commented these away, setting the GL version at createApplication()
        //Based on a quick search I couldn't figure out what would be the minimum gles version to support instanced rendering
        //Wikipedia says it was added at OpenGL ES 3.0 : https://en.wikipedia.org/wiki/OpenGL_ES#OpenGL_ES_3.0
        //And then there is an out-dated but once-official Apple documentation which states
        //"Instanced drawing is available in the core OpenGL ES 3.0 API and in OpenGL ES 2.0 through the EXT_draw_instanced and EXT_instanced_arrays extensions."
        //But I have no idea how to enable such an extension, and if it makes a difference depending on if the mac device supports Metal or not, so there is still a lot to learn!

        //this might be better if available - but is there a way to check availability at this stage?
        //configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,4,1);

        //but do we need this for mac compatibility?
        //configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30,3,2);


        configuration.useVsync(false);
        //// Limits FPS to the refresh rate of the currently active monitor.
        //configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.


        configuration.setBackBufferConfig(8,8,8,8,32,0,4);
        //for anti-aliasing we set the samples to 4
        //depth if also increased to 32 making long-range depth test more accurate
        //not using anti-aliasing might improve performance at the cost of quality.
        //this seems to cause problems on my hardware, so if you encounter strange crashes try commenting out line 39

        configuration.setDecorated(true);
        configuration.setResizable(true);
        //configuration.setHdpiMode(HdpiMode.Pixels);
        //there are probably default values, but setting them just to be sure

        Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        configuration.setWindowedMode((int) (displayMode.width*0.8f), (int) (displayMode.height*+0.8f));

        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
