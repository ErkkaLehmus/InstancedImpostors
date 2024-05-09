package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.enormouselk.sandbox.impostors.terrains.HeightMapTerrain;
import com.enormouselk.sandbox.impostors.terrains.Terrain;

public class DemoScreenCPU implements Screen {

    private static final int terrainHeightMultiplier = 32;
    public static final int TREE_TYPES_MAX = 3;
    public static final int TREE_TYPE_FIR = 0;
    public static final int TREE_TYPE_PINE = 1;
    public static final int TREE_TYPE_BIRCH = 2;
    private static final int LOD_MAX = 3;
    //a helper variable, we need this to draw stats


    private final String distanceTemplate = "%d m";
    private final String percentTemplate = "%d %%";

    private boolean disableRendering;

    private GLProfiler profiler;
    private InstancedShaderProvider instancedShaderProvider;
    private Environment environment;

    private Terrain terrain;
    private Renderable terrainRenderable;
    private ModelBatch batch;
    private SpriteBatch batch2D;
    private BitmapFont font;
    private PerspectiveCamera camera;
    private FirstPersonCameraController controller;
    private Frustum camFrustum;
    private long startTime, updateTime, renderTime;

    private String cachedDecalDistance;
    private String cachedDecalPercentDistance;


    //OK, it would be wise to make LodModelBatch and the basic LodModel to implement
    //a common base interface, so that it would be easy to switch between them in the code.
    //But that is not my top priority today...

    private final LodModelBatch[] lodModels = new LodModelBatch[TREE_TYPES_MAX];
    //private final LodModel[] lodModels = new LodModel[TREE_TYPES_MAX];

    private Model cabinModel;
    private ModelInstance cabinInstance;

    private Vector3 cabinPosition;


    private Vector3 vec3Temp;
    private Vector2 vec2Temp;
    private LodModelBatch lodModel;


    private final int tileSize = 10;
    private final int chunkSize = tileSize * 5;
    private int chunksX;
    private int chunksY;
    private int treesPerChunk;
    private int treeCount;

    private int counter;
    private float decalDistance;
    private int textureSize;
    private float maxDistance;

    private int[] treeTypes;
    //an array to hold the type of each tree


    //private Vector3[] treePositions;
    //an array to hold the position for each and every tree
    //private final int[] treeTypeInstanceCount = new int[TREE_TYPES_MAX];
    //and yet another array to store the amount of each tree type

    TreeChunk[][] world;


    private Array<LodSettings> lodSettings;


    private final ImpostorDemo owner;

    private boolean showTerrain;

    private int instanceBufferMaxSize;


    public DemoScreenCPU(ImpostorDemo owner) {
        this.owner = owner;
    }

    public void initGraphics(DemoEventListener listener, Array<LodSettings> lodSettings, int worldSize, int treeDensity, float decalDistance, int textureSize, int instanceBufferMaxSize, boolean showTerrain)
    {
        if (listener != null) listener.working("initializing...");

        this.showTerrain = showTerrain;
        this.instanceBufferMaxSize = instanceBufferMaxSize;

        maxDistance = worldSize * tileSize;
        counter = 0;

        this.lodSettings = new Array<>(lodSettings);

        Gdx.gl30.glEnable(GL30.GL_DEPTH_TEST);
        Gdx.gl30.glEnable(GL30.GL_CULL_FACE);
        //Gdx.gl30.glDisable(GL30.GL_CULL_FACE);
        Gdx.gl30.glCullFace(GL30.GL_BACK);
        Gdx.gl30.glDisable(GL30.GL_BLEND);


        disableRendering = true;
        chunksX = (int) (maxDistance / chunkSize);
        chunksY = chunksX;

        int tilesPerChunk = chunkSize / tileSize;
        treesPerChunk = treeDensity * tilesPerChunk * tilesPerChunk;

        this.textureSize = textureSize;
        this.decalDistance = decalDistance;

        treeCount = chunksX * chunksY * treesPerChunk;

        //treePositions = new Vector3[treeCount];
        treeTypes = new int[treeCount];

        world = new TreeChunk[chunksX][chunksY];
        init();
    }

    public void initLOD(DemoEventListener listener)
    {
        if (counter < lodSettings.size)
        {
            LodSettings s = lodSettings.get(counter);
            if (listener != null) listener.working("generating "+s.ID);

            //System.out.println("generating "+s.ID);

            lodModels[counter] = new LodModelBatch(s,instanceBufferMaxSize, decalDistance, maxDistance, textureSize, environment, instancedShaderProvider);
            //lodModels[counter] = new LodModelBatch(s,treeTypeInstanceCount[TREE_TYPE_FIR], decalDistance, maxDistance, textureSize, environment, instancedShaderProvider);
        }
        else {
             //cabinModel = new G3dModelLoader(new UBJsonReader()).loadModel(Gdx.files.internal("graphics/cabin.g3db"));

             cabinModel = LodModelBatch.loadModelFromGLTF("graphics/optimized/cabin_lod0.gltf");

             cabinInstance = new ModelInstance(cabinModel);

             if (listener != null) listener.finished();
        }

        counter++;
    }

    public void startDemo()
    {
        cacheDecalDistanceAsString();
        cacheDecalDistanceAsPercentString();
        createWorld();

        if (cabinInstance != null)
        {
            cabinInstance.transform.setTranslation(cabinPosition);
            cabinInstance.transform.rotate(Vector3.Y,180f);
            //cabinInstance.transform.setToLookAt(Vector3.Z,Vector3.Y);
        }

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
        ScreenUtils.clear(Color.SKY, true);
        checkUserInput();

        if (disableRendering) return;

        startTime = TimeUtils.nanoTime();
        updateInstancedData();
        updateTime = TimeUtils.timeSinceNanos(startTime);

        startTime = TimeUtils.nanoTime();
        batch.begin(camera);

        vec2Temp.set(camera.position.x,camera.position.z);

        //very simple chunk-based approach to optimize speed
        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {
                TreeChunk chunk = world[x][y];

                if (chunk == null) continue;

                if (camFrustum.boundsInFrustum(chunk.boundingBox))
                {
                    for (int i = 0; i < chunk.maxTrees; i++) {
                        TreeChunk.TreeInstance tree = chunk.treeInstances[i];
                        vec3Temp = tree.position;
                        lodModel = lodModels[tree.type];
                        lodModel.updateInstanceData(batch,camera.position,vec2Temp,vec3Temp);
                    }
                }
            }

        }

        //this was the old naive system checking each and every tree
        /*
        for (int i = 0; i < treeCount; i++) {
            vec3Temp = treePositions[i];
            lodModel = lodModels[treeTypes[i]];

            if (camFrustum.sphereInFrustum(vec3Temp,lodModel.radius))
                lodModel.updateInstanceData(batch,camera.position,vec2Temp,vec3Temp);
        }
         */

        for (int ii = 0; ii < TREE_TYPES_MAX; ii++) {
            lodModels[ii].pushInstanceData();
        }

        /*
        for (int ii = 0; ii < TREE_TYPES_MAX; ii++) {
            lodModels[ii].render(batch);
            batch.flush();
        }
         */


        if ((showTerrain) && (terrainRenderable != null)) batch.render(terrainRenderable);

        if (cabinInstance != null) batch.render(cabinInstance,environment);

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
        if (dist < 0)
            cachedDecalDistance = "disabled";
        else
            cachedDecalDistance =  String.format(distanceTemplate, (int)dist);
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
            font.draw(batch2D,lodModel.ID,10, 80+ii*32);
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
    public static int convertToTreeType(int val)
    {
        if (val < 45) return TREE_TYPE_FIR;
        if (val < 80) return TREE_TYPE_PINE;
        return TREE_TYPE_BIRCH;
    }


    private float getSubPosition()
    {
        return MathUtils.random((float)chunkSize);
    }

    private void createWorld(){
        //Noise noise = new Noise();
        Noise noise = new Noise(1337,1f/64f,Noise.SIMPLEX,1,2.5f,0.5f);
        float worldSizeHalf = chunksX * chunkSize / 2f;

        float elevations[] = new float[4];
        elevations[0] = noise.getConfiguredNoise(1f / tileSize,1f / tileSize)*terrainHeightMultiplier;
        elevations[1] = noise.getConfiguredNoise(1f / tileSize,9f / tileSize)*terrainHeightMultiplier;
        elevations[2] = noise.getConfiguredNoise(9f / tileSize,9f / tileSize)*terrainHeightMultiplier;
        elevations[3] = noise.getConfiguredNoise(9f / tileSize,1f / tileSize)*terrainHeightMultiplier;

        float cabinElevation = elevations[0];
        for (int i = 1; i < 4; i++) {
            if (elevations[i] > cabinElevation) cabinElevation = elevations[i];
        }

        cabinPosition = new Vector3(5f , cabinElevation+1f,5f);

        int tt;


        int worldSizeInTiles = MathUtils.round(maxDistance / tileSize);

        float[] heightMapData = new float[worldSizeInTiles * worldSizeInTiles];

        for (int x = 0; x < worldSizeInTiles; x++) {
            for (int y = 0; y < worldSizeInTiles ; y++) {

                float ix = x * tileSize - worldSizeHalf;
                float iz = y * tileSize - worldSizeHalf;

                heightMapData[y*worldSizeInTiles+x] =  noise.getConfiguredNoise(ix / tileSize, iz / tileSize) * terrainHeightMultiplier;
            }
        }

        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {


                float offX = x * chunkSize - (worldSizeHalf);
                float offY = y * chunkSize - (worldSizeHalf);

                //if ((offX == 0) && (offY == 0)) continue;;

                TreeChunk chunk = new TreeChunk(treesPerChunk,offX,offY);

                world[x][y] = chunk;

                for (int treeCounter = 0; treeCounter < treesPerChunk; treeCounter++) {

                    tt = convertToTreeType(MathUtils.random(100));

                    //treeTypes[counter] = tt;
                    //treeTypeInstanceCount[tt]++;

                    float ix = chunk.worldOffsetX + getSubPosition();
                    float iz = chunk.worldOffsetY + getSubPosition();

                    if ( ((ix > -5) && (ix < 15)) && ((iz > -5) && (iz < 15)) )
                    {
                        //too close to the cabin, relocate
                        ix += 32;
                        iz -= 32;
                    }

                    world[x][y].addInstance(tt,new Vector3(
                        ix ,
                        //noise.getConfiguredNoise((float)x / tilesX, (float)y / tilesY) * 128,
                        noise.getConfiguredNoise(ix / tileSize , iz / tileSize ) * terrainHeightMultiplier,
                        iz),
                        lodModels[tt].radius);

                    /*
                    treePositions[counter] = new Vector3(
                        (x*tileSize - (worldSize / 2) + getSubPosition()) ,
                        //noise.getConfiguredNoise((float)x / tilesX, (float)y / tilesY) * 128,
                        noise.getConfiguredNoise(x , y ) * 32,
                        (y*tileSize -  (worldSize / 2) + getSubPosition()) );

                     */

                    counter++;
                }
            }
        }

        if (showTerrain) {
            terrain = new HeightMapTerrain(heightMapData, worldSizeInTiles, 1f);
            terrainRenderable = terrain.getRenderable();

            ShaderProgram.prependVertexCode = "#version 100\n";
            ShaderProgram.prependFragmentCode = "#version 100\n";

            Shader terrainShader = new DefaultShader(terrainRenderable);
            terrainShader.init();
            terrainRenderable.shader = terrainShader;
        }
    }

    private void updateInstancedData(){

        for (int i = 0; i < TREE_TYPES_MAX; i++) {
            lodModels[i].resetInstanceData();
        }



    }


    private void takeCameraHome()
    {
        float startPos = chunkSize * (chunksY / 4);
        camera.position.set(0,128,-startPos);
        camera.up.set(Vector3.Y);
        camera.lookAt(0,0,0);
        camera.update();
    }

    private void init() {
        instancedShaderProvider = new InstancedShaderProviderCPU(null);

        vec2Temp = new Vector2();

        float lightDirY = -MathUtils.sinDeg(50);
        float lightDirZ = MathUtils.cosDeg(50);
        Vector3 dirVector = new Vector3(0,lightDirY,lightDirZ).nor();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.35f, 0.35f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 0.85f, 0, dirVector.y, dirVector.z));

        float maxVisibility = 2 * maxDistance;
        if (maxVisibility < 1000) maxVisibility = 1000;

        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = maxVisibility;
        takeCameraHome();
        camFrustum = camera.frustum;

        batch = new ModelBatch(instancedShaderProvider,null);
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

        if (cabinModel != null) cabinModel.dispose();

        if (terrain != null) terrain.dispose();

        instancedShaderProvider.dispose();
    }

    public interface DemoEventListener
    {
        public void finished();

        public void working(String message);
    }


}
