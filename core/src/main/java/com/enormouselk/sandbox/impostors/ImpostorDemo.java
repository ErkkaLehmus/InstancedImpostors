package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
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

    public void startDemo(float worldSize,float treeDensity,float decalDistance, int textureSize)
    {
        mainMenu.clear();
        if (demoScreen == null) demoScreen = new DemoScreen(ImpostorDemo.this);

        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                demoScreen.initGraphics(ImpostorDemo.this,(int)worldSize,(int)treeDensity,decalDistance,textureSize);
                handleDemoMessages = true;
            }
        });


        /*
        //start the demo after a while, so that the mainmenu has time to clear itself
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (demoScreen == null) demoScreen = new DemoScreen(ImpostorDemo.this);
                demoScreen.initGraphics(this,(int)worldSize,(int)treeDensity,decalDistance,textureSize);
                setScreen(demoScreen);
            }
        },0.25f);

         */


    }

    @Override
    public void finished() {
        handleDemoMessages = false;
        setScreen(demoScreen);
        demoScreen.startDemo();
    }

    @Override
    public void working(String message) {
        if (mainMenu!=null) mainMenu.addMessage(message);
    }

    public void stopDemo()
    {
        if (mainMenu == null) mainMenu = new MainMenu(this);
        setScreen(mainMenu);

        demoScreen.dispose();
        demoScreen=null;

    }
}
