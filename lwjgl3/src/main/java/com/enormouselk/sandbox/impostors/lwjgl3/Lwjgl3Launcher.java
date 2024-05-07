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

        ShaderProgram.prependFragmentCode = "#version 100\n";
        ShaderProgram.prependVertexCode = "#version 100\n";

        ImpostorDemo demo = new ImpostorDemo();
        Lwjgl3ApplicationConfiguration configuration = getDefaultConfiguration();
        Lwjgl3Application app;
        try {
            app = new Lwjgl3Application(demo,configuration);
        } catch (Exception e) {
            if (e.getMessage().contains("create window"))
            {
                //let's try a mac-friendly configuration
                configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32,3,2);
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

        //this might be better if available - but is there a way to check availability at this stage?
        configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32,4,1);

        //but do we need this for mac compatibility?
        //configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL32,3,2);


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
