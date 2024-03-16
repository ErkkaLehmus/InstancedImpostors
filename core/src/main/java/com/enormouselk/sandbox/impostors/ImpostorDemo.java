package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class ImpostorDemo extends Game implements DemoScreen.DemoEventListener {

    private MainMenu mainMenu;
    private DemoScreen demoScreen;

    private boolean handleDemoMessages;

    @Override
    public void create () {
        handleDemoMessages = false;
        mainMenu = new MainMenu(this);
        setScreen(mainMenu);
    }

    @Override
    public void resize (int width, int height) {

    }

    @Override
    public void render () {
        super.render();

        //Poor man's async loading - we initialize the LODs step by step
        //so that the demoScreen can post back messages of the progress
        //to be shown on the MainMenu
        if ((handleDemoMessages) && (demoScreen != null))
        {
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    demoScreen.initLOD(ImpostorDemo.this);
                }
            });
        }
    }

    @Override
    public void dispose () {
       if (mainMenu != null) mainMenu.dispose();
    }

    public void startDemo(Array<LodModel.LodSettings> settings, float worldSize, float treeDensity, float decalDistance, int textureSize)
    {
        mainMenu.clear();
        if (demoScreen == null) demoScreen = new DemoScreen(ImpostorDemo.this);

        demoScreen.initGraphics(ImpostorDemo.this,settings,  (int)worldSize,(int)treeDensity,decalDistance,textureSize);
        handleDemoMessages = true;
    }

    @Override
    public void finished() {
        handleDemoMessages = false;
        setScreen(demoScreen);
        mainMenu.dispose();
        mainMenu = null;
        demoScreen.startDemo();
    }

    @Override
    public void working(String message) {
        if (mainMenu!=null) mainMenu.addMessage(message);
    }

    public void stopDemo()
    {
        demoScreen.dispose();
        demoScreen = null;
        if (mainMenu == null) mainMenu = new MainMenu(this);
        setScreen(mainMenu);

        /*
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                demoScreen.dispose();
                demoScreen=null;
            }
        });

         */


    }
}
