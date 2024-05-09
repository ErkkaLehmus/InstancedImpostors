package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.*;
//import com.kotcrab..ui.UI;
//import com.kotcrab..ui.widget.*;

import java.nio.IntBuffer;
import java.util.Locale;

public class MainMenu implements Screen {

    private Stage stage;
    private final Skin skin;
    private Camera uiCam;

    private final int defaultWorldSize = 8;
    private final int absoluteMaxWorldSize = 32;
    private final double absoluteMaxTrees = Integer.MAX_VALUE;
    private int maxWorldSize = 32;
    private final int minWorldSize = 1;
    private final int defaultTreeDensity = 5;
    private final int maxTreeDensity = 9;
    private final int defaultImpostorDistance = 50;
    private final int defaultChunkSize = 4;
    private final int minChunkSize = 1;
    private final int maxChunkSize = 8;

    private final int defaultInstanceBufferSize = 32;
    private final int minInstanceBufferSize = 4;
    private final int maxInstanceBufferSize = 128;
    private final int instanceBufferBaseSize = 1024;

    private final String densityTemplate = "produces total %,d trees";
    private final String sizeTemplate = "%1$,d m x %1$,d m";
    private final String distanceTemplate = "%d %%";
    private final String amountTemplate = "%,d";

    private final Locale defaultLocale = Locale.ENGLISH;

    private final ImpostorDemo owner;

    //private Window window;

    Table root;
    private Table window;
    private TextButton startButton;
    private TextButton resetButton;
    private Slider sliderImpostorDistance;
    private Slider sliderTreeDensity;
    private Slider sliderWorldSize;
    private Slider sliderChunkSize;
    private Slider sliderBufferSize;
    private CheckBox checkBoxOptimized;
    private CheckBox checkBoxImpostors;
    private CheckBox checkBoxTerrain;
    private CheckBox checkBoxUse150;
    SelectBox<Integer> selectTextureSize;
    private Label legendImpostorDistance;
    private Label legendTreeDensity;
    private Label legendWorldSize;
    private Label legendInstanceBufferSize;
    private Label legendChunkSize;

    private Array<LodSettings> lodSettings;

    public MainMenu(ImpostorDemo owner) {
        super();
        this.owner = owner;
        skin = new Skin(Gdx.files.internal("metal-ui.json"));
        init();
    }

    public void init()
    {

        // Check if for GL30 profile
        if (Gdx.gl30 == null) {
            throw new GdxRuntimeException("GLES 3.0 profile required for this test");
        }

        lodSettings = new Array<>(3);


        //UI.setSkipGdxVersionCheck(true);
        //UI.load();
        //UI.load(UI.SkinScale.X1);
        //UI.setDefaultTitleAlign(Align.center);

        this.uiCam = new OrthographicCamera(Gdx.graphics.getWidth(),Gdx.graphics.getWidth());
        uiCam.translate(Gdx.graphics.getWidth()/2f,Gdx.graphics.getWidth()/2f, 0);
        uiCam.update();

        stage = new Stage(new ScreenViewport(uiCam));
        //stage = new Stage(new StretchViewport(1024,800));
        //stage = new Stage(new FillViewport(1024,1024,uiCam));

        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        //window = new Window("--- INSTANCED IMPOSTORS ---");
        window = new Table(skin);
        root.add(window).center();

        sliderWorldSize = new Slider(minWorldSize,maxWorldSize,1,false,skin);
        sliderWorldSize.setValue(defaultWorldSize);
        legendWorldSize = new Label(String.format(defaultLocale,sizeTemplate,defaultWorldSize * defaultChunkSize * 10),skin);
        //final Label legendWorldSize = new Label(" ");

        sliderTreeDensity = new Slider(1,maxTreeDensity,1,false,skin);
        sliderTreeDensity.setValue(defaultTreeDensity);
        legendTreeDensity = new Label(String.format(defaultLocale,densityTemplate,(defaultChunkSize*defaultWorldSize)*(defaultChunkSize*defaultWorldSize)* defaultTreeDensity),skin);
        //final Label legendTreeDensity = new Label("");

        sliderImpostorDistance = new Slider(10,100,10,false,skin);
        sliderImpostorDistance.setValue(defaultImpostorDistance);
        legendImpostorDistance = new Label(String.format(defaultLocale,distanceTemplate,defaultImpostorDistance),skin);

        sliderChunkSize = new Slider(minChunkSize,maxChunkSize,1,false,skin);
        sliderChunkSize.setValue(defaultChunkSize);
        legendChunkSize = new Label(String.format(defaultLocale,amountTemplate,convertSize(defaultChunkSize)),skin);

        sliderBufferSize = new Slider(minInstanceBufferSize,maxInstanceBufferSize,4,false,skin);
        sliderBufferSize.setValue(defaultInstanceBufferSize);
        legendInstanceBufferSize = new Label(String.format(defaultLocale,amountTemplate,defaultInstanceBufferSize * instanceBufferBaseSize),skin);

        selectTextureSize = new SelectBox<>(skin);
        selectTextureSize.setItems(getAvailableTextureSizes());
        selectTextureSize.setSelectedIndex(0);

        sliderWorldSize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setChunkAndWorldSize();
            }
        });

        sliderTreeDensity.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int worldSizeInChunks = (int) sliderWorldSize.getValue();
                int chunkSize = convertSize(sliderChunkSize.getValue());
                int worldSizeInTiles = worldSizeInChunks * chunkSize;
                int treeDensity = (int) sliderTreeDensity.getValue();
                legendTreeDensity.setText(String.format(defaultLocale,densityTemplate,(worldSizeInTiles * worldSizeInTiles * treeDensity)));
            }
        });

        sliderImpostorDistance.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                legendImpostorDistance.setText(String.format(defaultLocale,distanceTemplate,(int) sliderImpostorDistance.getValue()));
            }
        });

        sliderChunkSize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setChunkAndWorldSize();
            }
        });

        sliderBufferSize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                legendInstanceBufferSize.setText(String.format(defaultLocale,amountTemplate,(int) sliderBufferSize.getValue() * instanceBufferBaseSize));
            }
        });



        checkBoxOptimized = new CheckBox("Use optimized models",skin);
        checkBoxImpostors = new CheckBox("Use impostor decals",skin);
        checkBoxTerrain = new CheckBox("Show terrain",skin);
        checkBoxUse150 = new CheckBox("Revert to glsl version 150",skin);

        resetButton = new TextButton("reset to defaults",skin);
        resetButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                setDefaultPreset();
                //resetButton.focusLost();
            }
        });


        startButton = new TextButton("run demo",skin);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed (ChangeEvent event, Actor actor) {
                setDefaultModels(checkBoxOptimized.isChecked());
                owner.startDemo(lodSettings, sliderWorldSize.getValue(), sliderTreeDensity.getValue(), sliderImpostorDistance.getValue() / 100f,selectTextureSize.getSelected(), convertSize(sliderChunkSize.getValue()), (int) sliderBufferSize.getValue() * instanceBufferBaseSize,checkBoxTerrain.isChecked(),checkBoxUse150.isChecked());
            }
        });



    }

    private int convertSize(float raw)
    {
        int max = (int)raw;
        int ret = 4;
        for (int i = 0; i < max; i++) {
            ret = ret * 2;
        }
        return ret;
    }

    private void setDefaultModels(boolean optimized)
    {
        lodSettings.clear();

        boolean useImpostors = checkBoxImpostors.isChecked();

        if (optimized) {
            lodSettings.add(new LodSettings("graphics/optimized/fir_", "gltf", "FIR", 3, useImpostors , LodSettings.SHADERTYPE_MINIMAL, false));
            lodSettings.add(new LodSettings("graphics/optimized/pine_", "gltf","PINE", 3, useImpostors , LodSettings.SHADERTYPE_MINIMAL, false));
            lodSettings.add(new LodSettings("graphics/optimized/birch_", "gltf","BIRCH", 3, useImpostors , LodSettings.SHADERTYPE_MINIMAL, false));
        }
        else
        {
            lodSettings.add(new LodSettings("graphics/fir-", "glb","FIR", 3, useImpostors , LodSettings.SHADERTYPE_MINIMAL, false));
            lodSettings.add(new LodSettings("graphics/pine-", "glb","PINE", 3, useImpostors , LodSettings.SHADERTYPE_MINIMAL, false));
            lodSettings.add(new LodSettings("graphics/birch-", "glb","BIRCH", 3, useImpostors , LodSettings.SHADERTYPE_MINIMAL, false));
        }
    }

    private void setChunkAndWorldSize()
    {
        int chunkSize = convertSize(sliderChunkSize.getValue());

        legendChunkSize.setText(String.format(defaultLocale,amountTemplate,chunkSize));
        int worldSizeInChunks = (int) sliderWorldSize.getValue();

        int worldSizeInTiles = worldSizeInChunks * chunkSize;
        int worldSize = worldSizeInTiles*10;
        int treeDensity = (int) sliderTreeDensity.getValue();

        int computeMax = (int)Math.sqrt(absoluteMaxTrees / treeDensity);
        long trees = (long) worldSizeInTiles * worldSizeInTiles * treeDensity;
        if (trees > absoluteMaxTrees)
        {
            maxWorldSize = computeMax / chunkSize;
            sliderWorldSize.setValue(maxWorldSize);

            worldSizeInChunks = maxWorldSize;

            worldSizeInTiles = worldSizeInChunks * chunkSize;
            worldSize = worldSizeInTiles*10;

            trees = worldSizeInTiles * worldSizeInTiles * treeDensity;
        }

        legendWorldSize.setText(String.format(defaultLocale,sizeTemplate,worldSize));
        legendTreeDensity.setText(String.format(defaultLocale,densityTemplate,trees));
    }

    @Override
    public void show() {

        stage.getViewport().apply();
        Gdx.input.setInputProcessor(stage);

        window.clear();

        window.defaults().pad(4);

        window.add("If you plan anything like a 3D game with lots of objects you can try different parameters to find out how much stuff").colspan(3).row();
        window.add("there can be without impairing the performance.").padTop(2).colspan(3).row();
        window.add("Maximum OpenGL version supported by your device: "+getMaxOpenGLVersion()).padTop(2).colspan(3).row();
        window.add("Maximum amount of trees is capped to "+String.format(defaultLocale,amountTemplate,(int)absoluteMaxTrees)).padTop(2).colspan(3).row();

        window.add(" ");
        window.add("F1 = HELP").row();

        window.add(" ");
        window.add("1 = Low end preset").row();

        window.add(" ");
        window.add("2 = Reset default settings").row();

        window.add(" ");
        window.add("3 = Max everything").row();

        window.add(" ");
        window.add("4 = Unoptimized preset").row();

        window.add("Tree density : ").right().padTop(32);
        window.add(sliderTreeDensity).growX().padTop(32);
        window.add(legendTreeDensity).left().padTop(32).row();

        window.add("Chunk size : ").right();
        window.add(sliderChunkSize).growX();
        window.add(legendChunkSize).left().row();

        window.add("World size : ").right();
        window.add(sliderWorldSize).growX();
        window.add(legendWorldSize).left().row();

        window.add("Impostor distance : ").right();
        window.add(sliderImpostorDistance).growX();
        window.add(legendImpostorDistance).left().row();

        window.add("Buffer size : ").right();
        window.add(sliderBufferSize).growX();
        window.add(legendInstanceBufferSize).left().row();


        window.add("Texture size :").right();
        window.add(selectTextureSize).left().row();

        checkBoxOptimized.setChecked(true);
        window.add(" ");
        window.add(checkBoxOptimized).left().row();

        window.add(" ");
        checkBoxImpostors.setChecked(true);
        window.add(checkBoxImpostors).left().row();

        window.add(" ");
        checkBoxTerrain.setChecked(true);
        window.add(checkBoxTerrain).left().row();

        window.add(" ");
        checkBoxUse150.setChecked(false);
        window.add(checkBoxUse150).left().row();

        window.add(resetButton).padTop(32).padBottom(32).padRight(32);
        window.add(startButton).padTop(32).padBottom(32).row();

        //window.add("Impostor distance determines at what distance 3D models will be displayed at 2D impostors.").colspan(3).row();
       

        window.setBackground("cyan");
        window.pack();



        //sliderWorldSize.setValue(defaultWorldSize);
        //sliderTreeDensity.setValue(defaultTreeDensity);

        //root.debugAll();
    }

    private void setLowEndPreset()
    {
        sliderBufferSize.setValue(32f);
        sliderChunkSize.setValue(1f);
        sliderWorldSize.setValue(16f);
        sliderTreeDensity.setValue(5f);
        sliderImpostorDistance.setValue(10f);
        
        checkBoxTerrain.setChecked(true);
        checkBoxOptimized.setChecked(true);
        checkBoxImpostors.setChecked(true);
        
    }
    
    private void setDefaultPreset()
    {
        sliderWorldSize.setValue(defaultWorldSize);
        sliderChunkSize.setValue(defaultChunkSize);
        sliderTreeDensity.setValue(defaultTreeDensity);
        sliderImpostorDistance.setValue(defaultImpostorDistance);
        sliderBufferSize.setValue(defaultInstanceBufferSize);
        selectTextureSize.setSelectedIndex(0);

        checkBoxTerrain.setChecked(true);
        checkBoxOptimized.setChecked(true);
        checkBoxImpostors.setChecked(true);
    }

    private void setMaxPreset()
    {
        sliderWorldSize.setValue(15);
        sliderChunkSize.setValue(maxChunkSize);
        sliderTreeDensity.setValue(maxTreeDensity);
        sliderImpostorDistance.setValue(100);
        sliderBufferSize.setValue(maxInstanceBufferSize);
        selectTextureSize.setSelectedIndex(0);

        checkBoxTerrain.setChecked(true);
        checkBoxOptimized.setChecked(true);
        checkBoxImpostors.setChecked(true);
    }

    private void setUnoptimizedPreset()
    {
        sliderWorldSize.setValue(minWorldSize);
        sliderChunkSize.setValue(maxChunkSize);
        sliderTreeDensity.setValue(maxTreeDensity);
        sliderImpostorDistance.setValue(100);
        sliderBufferSize.setValue(maxInstanceBufferSize);
        selectTextureSize.setSelectedIndex(0);

        checkBoxTerrain.setChecked(true);
        checkBoxOptimized.setChecked(false);
        checkBoxImpostors.setChecked(false);
    }
    
    private void showHelp()
    {
        Dialog helpDialog = new Dialog("HELP",skin);

        helpDialog.getContentTable().add("The world is made up of chunks. Model culling is not performed per tree, but per chunk").colspan(3).padTop(16).row();
        helpDialog.getContentTable().add("If the world is relatively small, say 1000 m, you could set chunk size to 1000m and world size to 1.").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("Also, LOD level is chosen per chunk, not per individual tree.").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("If you wish to test if LOD optimization improves performance, you need to set chunk size smaller.").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("Although I'd guess that for a modern gpu LOD optimization would matter only if the tree models were.").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("lot more detailed. I've been planning a GUI to pick models from your local hard drive, but didn't have time for that.").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("So experimenting with more complex models requires editing the source code.").colspan(3).padTop(4).row();

        helpDialog.getContentTable().add("The impostor distance is relative to the chosen world size, so if your world size is 1000 m").colspan(3).padTop(16).row();
        helpDialog.getContentTable().add("and Impostor distance is 50% models more than 500m from camera are displayed as impostors.").colspan(3).padTop(4).row();

        helpDialog.getContentTable().add("Increasing instance buffer size might improve performance but is sure to eat more memory.").colspan(3).padTop(16).row();

        helpDialog.getContentTable().add("3D models have also their own LOD versions, and the demo uses 3 levels;").colspan(3).padTop(16).row();
        helpDialog.getContentTable().add("LOD0 = full detail, for objects closer than 1/4 of impostor distance").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("LOD1 = medium detail, for objects closer than 1/2 of impostor distance").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("LOD2 = reduced detail, for objects closer than impostor distance").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("Impostor = an image of 3D model flattened to 2D surface").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("Each impostor uses one texture of the given size - the bigger the size the better the quality.").colspan(3).padTop(4).row();

        helpDialog.getContentTable().add("By default the demo uses glsl version 300, which is for GL ES 3.0").colspan(3).padTop(16).row();
        helpDialog.getContentTable().add("Looks like some devices support instanced rendering in their OpenGL3.2, but are not able to fully emulate GL ES 3.0").colspan(3).padTop(4).row();
        helpDialog.getContentTable().add("In such cases it might help to revert to glsl version 150 which corresponds to OpenGL 3.2").colspan(3).padTop(4).row();

        helpDialog.getContentTable().add("- 9th of May 2024 Erkka Lehmus / Enormous Elk -").colspan(3).padTop(16).padBottom(32).row();

        helpDialog.button("OK");

        helpDialog.show(stage);
    }

    @Override
    public void render(float delta) {
        //Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        //Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        handleInput();

        ScreenUtils.clear(Color.SKY);

        stage.getViewport().apply();
        stage.act(delta);
        //stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }
    
    private void handleInput()
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) showHelp();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) setLowEndPreset();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) setDefaultPreset();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) setMaxPreset();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) setUnoptimizedPreset();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width,height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        //dispose();
    }

    @Override
    public void dispose() {
        if (stage == null) return;

        //UI.dispose();
        skin.dispose();
        stage.dispose();
        stage = null;
    }

    public void clear(boolean useOptimized)
    {
        window.clearChildren();
        window.setBackground("cyan");
        window.setSkin(skin);
        //window.getTitleLabel().setText("... PLEASE WAIT ...");
        window.add("Generating 3D, this might take a while.").row();
        window.add("Or this might crash, if there is not enough memory.").row();
        window.add("Let's hope for the best!").row();

        if (useOptimized)
        {
            window.add("Using optimized models.").row();
        }
        else
        {
            window.add("Using UNOPTIMIZED models.").row();
        }

        window.pack();
        window.invalidateHierarchy();

        //root.debugAll();
        //window.centerWindow();
    }

    public void addMessage(String message)
    {
        stage.getViewport().apply();
        window.add(message).row();
        window.pack();
        window.invalidateHierarchy();
    }

    private int getMaxTextureSize () {
        IntBuffer buffer = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL30.GL_MAX_TEXTURE_SIZE, buffer);
        int ret = buffer.get(0);
        return Math.min(ret,4096);
    }

    private String getMaxOpenGLVersion()
    {
        return Gdx.gl.glGetString(GL30.GL_VERSION);
    }

    private Array<Integer> getAvailableTextureSizes()
    {
        Array<Integer> ret = new Array<>();
        int max = getMaxTextureSize();
        int base = 1024;
        while (base <= max)
        {
            ret.add(base);
            base = base *2;
        }
        return ret;
    }

}
