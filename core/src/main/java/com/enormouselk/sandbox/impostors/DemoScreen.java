package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL32;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Arrays;

public class DemoScreen implements Screen {

    public static final int TREE_TYPES_MAX = 3;
    public static final int TREE_TYPE_FIR = 0;
    public static final int TREE_TYPE_PINE = 1;
    public static final int TREE_TYPE_BIRCH = 2;
    private static final int LOD_MAX = 3;
    //a helper variable, we need this to draw stats

    private static final String[] TREE_TITLES = {"FIR","PINE","BIRCH"};

    private final String distanceTemplate = "%d m";
    private final String percentTemplate = "%d %%";

    private boolean disableRendering;

    private GLProfiler profiler;
    private InstancedShaderProvider instancedShaderProvider;
    private Environment environment;
    private ModelBatch batch;
    private SpriteBatch batch2D;
    private BitmapFont font;
    private PerspectiveCamera camera;
    private FirstPersonCameraController controller;
    private Frustum camFrustum;
    private long startTime, updateTime, renderTime;

    private String cachedDecalDistance;
    private String cachedDecalPercentDistance;


    private final LodModel[] lodModels = new LodModel[TREE_TYPES_MAX];
    //private Quaternion q;
    //private Matrix4 mat4;
    private Vector3 vec3Temp;
    private Vector2 vec2Temp;
    private LodModel lodModel;


    private final int tileSize = 10;
    private int tilesX;
    private int tilesY;
    private int treesPerTile;
    private int treeCount;

    private int counter;
    private float decalDistance;
    private int textureSize;
    private float maxDistance;

    private Vector3[] treePositions;
    //an array to hold the position for each and every tree
    private int[] treeTypes;
    //another array to hold the type of each tree
    private final int[] treeTypeInstanceCount = new int[TREE_TYPES_MAX];
    //and yet another array to store the amount of each tree type

    private Array<LodModel.LodSettings> lodSettings;


    private final ImpostorDemo owner;


    public DemoScreen(ImpostorDemo owner) {
        this.owner = owner;
    }

    public void initGraphics(DemoEventListener listener, Array<LodModel.LodSettings> lodSettings, int worldSize, int treeDensity, float decalDistance, int textureSize)
    {
        if (listener != null) listener.working("initializing...");

        this.lodSettings = new Array<>(lodSettings);

        Gdx.gl32.glEnable(GL32.GL_DEPTH_TEST);
        Gdx.gl32.glEnable(GL32.GL_CULL_FACE);
        Gdx.gl32.glCullFace(GL32.GL_BACK);

        disableRendering = true;
        tilesX = worldSize;
        tilesY = worldSize;
        treesPerTile = treeDensity;

        this.textureSize = textureSize;
        this.decalDistance = decalDistance;

        treeCount = tilesY * tilesX * treesPerTile;

        treePositions = new Vector3[treeCount];
        treeTypes = new int[treeCount];

        init();
        createTreePositions();

        maxDistance = worldSize * tileSize;
        counter = 0;
    }

    public void initLOD(DemoEventListener listener)
    {
        if (counter < lodSettings.size)
        {
            LodModel.LodSettings s = lodSettings.get(counter);
            if (listener != null) listener.working("generating "+s.ID);
            lodModels[counter] = new LodModel(s,treeTypeInstanceCount[TREE_TYPE_FIR], decalDistance, maxDistance, textureSize, environment, instancedShaderProvider);
        }
        else if (listener != null) listener.finished();

        /*
        switch (counter) {
            case 0:
                if (listener != null) listener.working("generating fir trees");
                lodModels[TREE_TYPE_FIR] = new LodModel("graphics/fir", "FIR", LOD_MAX, true,treeTypeInstanceCount[TREE_TYPE_FIR], decalDistance, maxDistance, textureSize, environment, instancedShaderProvider);
                break;

            case 1:
                if (listener != null) listener.working("generating pine trees");
                lodModels[TREE_TYPE_PINE] = new LodModel("graphics/pine", "PINE", LOD_MAX, true,treeTypeInstanceCount[TREE_TYPE_PINE], decalDistance, maxDistance, textureSize, environment, instancedShaderProvider);
                break;

            case 2:
                if (listener != null) listener.working("generating birch trees");
                lodModels[TREE_TYPE_BIRCH] = new LodModel("graphics/birch", "BIRCH", LOD_MAX, false, treeTypeInstanceCount[TREE_TYPE_BIRCH], decalDistance, maxDistance, textureSize, environment, instancedShaderProvider);
                break;
            default:
                if (listener != null) listener.finished();
                break;
        }

         */
        counter++;
    }

    public void startDemo()
    {
        cacheDecalDistanceAsString();
        cacheDecalDistanceAsPercentString();
        disableRendering = false;
    }


    private void recalculateLODdistances()
    {
        for (int i = 0; i < LOD_MAX; i++) {
            lodModels[i].initLodDistances(decalDistance * maxDistance);
            lodModels[i].reallocBuffers(decalDistance);

            cacheDecalDistanceAsPercentString();
            cacheDecalDistanceAsString();
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        profiler.reset();

        controller.update();
        Gdx.gl32.glEnable(GL32.GL_DEPTH_TEST);
        Gdx.gl32.glEnable(GL32.GL_CULL_FACE);
        Gdx.gl32.glCullFace(GL32.GL_BACK);
        Gdx.gl32.glDisable(GL32.GL_BLEND);
        ScreenUtils.clear(Color.SKY, true);
        checkUserInput();

        if (disableRendering) return;

        startTime = TimeUtils.nanoTime();
        updateInstancedData();
        updateTime = TimeUtils.timeSinceNanos(startTime);

        startTime = TimeUtils.nanoTime();
        batch.begin(camera);

        for (int ii = 0; ii < TREE_TYPES_MAX; ii++) {
            lodModels[ii].render(batch);
            batch.flush();
        }
        batch.end();
        renderTime = TimeUtils.timeSinceNanos(startTime);

        // 2D HUD to show stats
        batch2D.begin();
        drawStats();
        batch2D.end();

    }

    private void cacheDecalDistanceAsPercentString()
    {
        cachedDecalPercentDistance = String.format(percentTemplate, (int)(decalDistance * 100));
        //return ((int)decalDistance * 100)+" %";
    }

    private void cacheDecalDistanceAsString()
    {
        int dist = (int)lodModels[0].getDecalDistance();
        if (dist < 0) cachedDecalDistance = "disabled";
        cachedDecalDistance =  String.format(distanceTemplate, (int)dist);
        //return ((int)decalDistance * 100)+" %";
    }

    private String getDecalDistanceAsPercentString()
    {
       return cachedDecalPercentDistance;
    }

    private String getDecalDistanceAsString()
    {
        return cachedDecalDistance;
    }

    private void drawStats() {
        font.draw(batch2D,"Q W E A S D + mouse drag: move camera" ,10,Gdx.graphics.getHeight()- 32);
        font.draw(batch2D,"Z X : adjust LOD distances" ,10,Gdx.graphics.getHeight()- 32-32);
        font.draw(batch2D,"HOME : reset camera" ,10,Gdx.graphics.getHeight()- 32 - 64);
        font.draw(batch2D,"ESC : quit to main menu" ,10,Gdx.graphics.getHeight()- 32 - 96);

        font.draw(batch2D,"DECAL DISTANCE : " + getDecalDistanceAsPercentString() +" = " +getDecalDistanceAsString(), 10, 192);
        for (int ii = 0; ii < 3; ii++) {
            lodModel = lodModels[ii];
            font.draw(batch2D,TREE_TITLES[ii],10, 80+ii*32);
            for (int i = 0; i < LOD_MAX; i++) {
                font.draw(batch2D, "LOD"+i+": " + lodModel.lodCount[i] , 140 + (i*220), 80+ii*32);
            }
            font.draw(batch2D,"DECALS: " + lodModel.decalCount , 140 + (LOD_MAX*220), 80+ii*32);
        }

        font.draw(batch2D,"Update Time: " + TimeUtils.nanosToMillis(updateTime) + "ms   Render Time: " + TimeUtils.nanosToMillis(renderTime) + "ms", 10, 232);
        font.draw(batch2D,"FPS: " + Gdx.graphics.getFramesPerSecond() +
                "  Draw Calls: " + profiler.getDrawCalls() +
               "  Vert Count: " + profiler.getVertexCount().latest +
                "  Shader Switches: " + profiler.getShaderSwitches() +
                "  Texture Bindings: " + profiler.getTextureBindings(),
            10, 264);
    }

    private void checkUserInput() {

        camera.up.set(Vector3.Y);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
            disableRendering = true;
            owner.stopDemo();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.HOME))
        {
            takeCameraHome();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Z))
        {
            if (decalDistance > 0.1f)
            {
                decalDistance -= 0.1f;
                recalculateLODdistances();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X))
        {
            if (decalDistance < 1f)
            {
                decalDistance += 0.1f;
                recalculateLODdistances();
            }
        }


    }


    /**
     * A simple helper function to convert the given value to a tree type
     * @param val range 0 - 100
     * @return an int referring to a tree type
     */
    private int convertToTreeType(int val)
    {
        if (val < 45) return TREE_TYPE_FIR;
        if (val < 80) return TREE_TYPE_PINE;
        return TREE_TYPE_BIRCH;
    }


    private float getSubPosition()
    {
        return MathUtils.random((float)tileSize);
    }

    private void createTreePositions(){
        Noise noise = new Noise();
        float worldSize = tilesX * tileSize;

        int counter = 0;
        int tt;

        //I'm old school, I like to explicitly initialize my variables although I'd guess this is not necessary nowadays?
        Arrays.fill(treeTypeInstanceCount, 0);

        for (int x = 0; x < tilesX; x++) {
            for (int y = 0; y < tilesY; y++) {
                for (int z = 0; z < treesPerTile; z++) {

                    tt = convertToTreeType(MathUtils.random(100));

                    treeTypes[counter] = tt;
                    treeTypeInstanceCount[tt]++;

                    treePositions[counter] = new Vector3(
                        (x*tileSize - (worldSize / 2) + getSubPosition()) ,
                        //noise.getConfiguredNoise((float)x / tilesX, (float)y / tilesY) * 128,
                        noise.getConfiguredNoise(x , y ) * 32,
                        (y*tileSize -  (worldSize / 2) + getSubPosition()) );

                    counter++;
                }
            }
        }
    }

    private void updateInstancedData(){
        for (int i = 0; i < TREE_TYPES_MAX; i++) {
            lodModels[i].resetInstanceData();
        }

        vec2Temp.set(camera.position.x,camera.position.z);
        for (int i = 0; i < treeCount; i++) {
            vec3Temp = treePositions[i];
            lodModel = lodModels[treeTypes[i]];

            if (camFrustum.sphereInFrustum(vec3Temp,lodModel.radius))
                lodModel.updateInstanceData(camera.position,vec2Temp,vec3Temp);
        }

        for (int ii = 0; ii < TREE_TYPES_MAX; ii++) {
            lodModels[ii].pushInstanceData();
        }
    }


    private void takeCameraHome()
    {
        float startPos = tileSize * (tilesY / 4);
        camera.position.set(0,128,-startPos);
        camera.up.set(Vector3.Y);
        camera.lookAt(0,0,0);
        camera.update();
    }

    private void init() {
        instancedShaderProvider = new InstancedShaderProvider(null);

        vec2Temp = new Vector2();

        float lightDirY = -MathUtils.sinDeg(50);
        float lightDirZ = MathUtils.cosDeg(50);
        Vector3 dirVector = new Vector3(0,lightDirY,lightDirZ).nor();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.35f, 0.35f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 0.85f, 0, dirVector.y, dirVector.z));

        float maxVisibility = 2 * tilesX * tileSize;
        if (maxVisibility < 1000) maxVisibility = 1000;

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = maxVisibility;
        takeCameraHome();
        camFrustum = camera.frustum;

        batch = new ModelBatch();
        batch2D = new SpriteBatch();

        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        font.setColor(Color.WHITE);
        font.getData().setScale(2);

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(tileSize*6);
        controller.setDegreesPerPixel(1f);
        Gdx.input.setInputProcessor(controller);

        //create & enable the profiler
        profiler = new GLProfiler(Gdx.graphics);
        profiler.enable();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose () {

        font.dispose();
        batch.dispose();

        batch2D.dispose();

        for (int i = 0; i < 3; i++) {
            lodModels[i].dispose();
        }

        instancedShaderProvider.dispose();
    }

    public interface DemoEventListener
    {
        public void finished();

        public void working(String message);
    }


}
