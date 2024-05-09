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
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
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

public class DemoScreen implements Screen {

    static final int terrainHeightMultiplier = 64;
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
    private InstancedShaderProviderGPU instancedShaderProvider;
    Environment environment;

    //private Terrain terrain;
    //private Renderable terrainRenderable;
    private ModelBatch batch;
    private SpriteBatch batch2D;
    private RenderContext context;
    private InstancedShaderProviderGPU.InstancedShader instancedTreeShader;
    private InstancedShaderProviderGPU.ImpostorShader impostorShader;
    private InstancedShaderProviderGPU.ImpostorShaderGPUheavy impostorShaderGPUheavy;
    private Shader terrainShader;

    private BitmapFont font;
    private PerspectiveCamera camera;
    private FirstPersonCameraController controller;
    private Frustum camFrustum;
    private long startTime, updateTime, renderTime;
    private long treesTotal;

    private String cachedDecalDistance;
    private String cachedDecalPercentDistance;


    //OK, it would be wise to make LodModelBatch and the basic LodModel to implement
    //a common base interface, so that it would be easy to switch between them in the code.
    //But that is not my top priority today...

    final LodModel[] lodModels = new LodModel[TREE_TYPES_MAX];

    private Model cabinModel;
    private ModelInstance cabinInstance;

    private Vector3 cabinPosition;


    private Vector2 cameraLocation2D;
    private LodModel lodModel;


    final int tileSize = 10;
    private int chunkSize;
    private int tilesPerChunk;
    private int chunksX;
    private int chunksY;
    private int treesPerChunk;
    int treesPerTile;
    private int treeCount;

    private int counter;
    private float decalDistance;
    private int textureSize;
    private float maxDistance;
    public float worldSizeHalf;

    private int[] treeTypes;
    //an array to hold the type of each tree



    MapChunk[][] world;
    Array<MapChunk> chunksToBeRendered;
    Array<MapChunk> chunksToBeRenderedAsImpostors;
    Array<MapChunk> chunksToBeRenderedAsGPUheavyImpostors;

    private float GPUheavyThreshold;


    private Array<LodSettings> lodSettings;


    private final ImpostorDemo owner;

    boolean showTerrain;

    private int instanceBufferMaxSize;

    private int bufferSize;


    public DemoScreen(ImpostorDemo owner) {
        this.owner = owner;
    }

    public void initGraphics(DemoEventListener listener, Array<LodSettings> lodSettings, int worldSize, int treeDensity, float decalDistance, int textureSize, int chunkSizeInTiles, int bufferSize, boolean showTerrain, boolean use150)
    {
        if (listener != null) listener.working("initializing...");

        this.showTerrain = showTerrain;
        this.tilesPerChunk = chunkSizeInTiles;
        this.chunkSize = chunkSizeInTiles * tileSize;
        this.treesPerTile = treeDensity;
        this.bufferSize = bufferSize * 3;

        maxDistance = worldSize *  chunkSizeInTiles * tileSize;
        worldSizeHalf = maxDistance / 2f;
        counter = 0;

        GPUheavyThreshold = 1000f;

        this.lodSettings = new Array<>(lodSettings);
        disableRendering = true;

        chunksX = worldSize;
        chunksY = worldSize;


        treesPerChunk = treeDensity * tilesPerChunk * tilesPerChunk;
        instanceBufferMaxSize = treesPerChunk;

        this.textureSize = textureSize;
        this.decalDistance = decalDistance;

        treeCount = chunksX * chunksY * treesPerChunk;

        //treePositions = new Vector3[treeCount];
        treeTypes = new int[treeCount];

        world = new MapChunk[chunksX][chunksY];
        chunksToBeRendered = new Array<>(chunksX*chunksY);
        chunksToBeRenderedAsImpostors = new Array<>(chunksX*chunksY);
        chunksToBeRenderedAsGPUheavyImpostors = new Array<>(chunksX*chunksY);

        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {
                world[x][y] = new MapChunk(chunkSize,TREE_TYPES_MAX,treesPerChunk,x * chunkSize - worldSizeHalf, y*chunkSize - worldSizeHalf);
            }
        }

        init(use150);
    }

    public void initLOD(DemoEventListener listener)
    {
        if (counter < lodSettings.size)
        {
            LodSettings s = lodSettings.get(counter);
            if (listener != null) listener.working("generating "+s.ID);

            //System.out.println("generating "+s.ID);

            lodModels[counter] = new LodModel(s,instanceBufferMaxSize, decalDistance, maxDistance, textureSize, bufferSize, environment, instancedShaderProvider);
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

        instancedTreeShader.setRenderable(lodModels[0].renderables[0]);
        instancedTreeShader.init();
        //instancedTreeShader.setEnvironment(environment);

        lodModel = lodModels[0];

        if (lodModel.getDecalDistance() > 0) {
            impostorShader.init();
            impostorShader.init(impostorShader.program, lodModel.renderables[lodModel.decalIndex]);

            impostorShaderGPUheavy.init();
            impostorShaderGPUheavy.init(impostorShaderGPUheavy.program, lodModel.renderables[lodModel.decalIndex]);
        }

        context = new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1));

        cacheDecalDistanceAsString();
        cacheDecalDistanceAsPercentString();
        createWorld();

        if (cabinInstance != null)
        {
            cabinInstance.transform.setTranslation(cabinPosition);
            //cabinInstance.transform.rotate(Vector3.Y,180f);
            //cabinInstance.transform.setToLookAt(Vector3.Z,Vector3.Y);
        }

        disableRendering = false;
        takeCameraHome();
    }


    private void recalculateLODdistances()
    {
        for (int i = 0; i < LOD_MAX; i++) {
            lodModels[i].initLodDistances(decalDistance * maxDistance);

            cacheDecalDistanceAsPercentString();
            cacheDecalDistanceAsString();
        }
    }

    @Override
    public void show() {
        Gdx.gl30.glEnable(GL30.GL_DEPTH_TEST);
        Gdx.gl30.glEnable(GL30.GL_CULL_FACE);
        //Gdx.gl30.glDisable(GL30.GL_CULL_FACE);
        Gdx.gl30.glCullFace(GL30.GL_BACK);
        Gdx.gl30.glDisable(GL30.GL_BLEND);
    }

    @Override
    public void render(float delta) {
        profiler.reset();

        controller.update();
        ScreenUtils.clear(Color.SKY, true);
        checkUserInput();

        if (disableRendering) return;

        startTime = TimeUtils.nanoTime();
        resetCounters();
        updateTime = TimeUtils.timeSinceNanos(startTime);

        startTime = TimeUtils.nanoTime();

        for (int i = 0; i < lodModels.length; i++) {
            lodModels[i].clearInstanceData();
        }

        //chunk-based approach to optimize speed

        chunksToBeRendered.clear();
        chunksToBeRenderedAsImpostors.clear();
        chunksToBeRenderedAsGPUheavyImpostors.clear();
        //first we determine which chunks are visible
        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {
                MapChunk chunk = world[x][y];

                if (camFrustum.boundsInFrustum(chunk.boundingBox)) {
                    chunk.getDistance(camera.position);
                    chunksToBeRendered.add(chunk);
                }
            }

        }

        context.begin();

        boolean haveModels = false;



        for (MapChunk mapChunk : chunksToBeRendered) {
            for (int i = 0; i < TREE_TYPES_MAX; i++) {

                float[] data = mapChunk.getTreeTypePositions(i);
                if (data == null) continue;

                LodModel lodModel = lodModels[i];
                int lodIndex = lodModel.getLODlevel(mapChunk.distanceFromCamera);

                if (lodIndex < lodModel.LOD_MAX) {

                    if (!haveModels) {
                        instancedTreeShader.begin(camera, context);
                        instancedTreeShader.setEnvironment(environment);
                        haveModels = true;
                    }

                    lodModel.addInstanceData(lodIndex, data);
                    //lodModel.render(instancedTreeShader,lodIndex,data);
                }
                else {

                    if (mapChunk.distanceFromCamera < GPUheavyThreshold)
                        chunksToBeRenderedAsGPUheavyImpostors.add(mapChunk);
                    else
                        chunksToBeRenderedAsImpostors.add(mapChunk);

                }
                //batch.flush();

            }
        }

        if (haveModels) {

            for (int i = 0; i < lodModels.length; i++) {
                lodModels[i].flushInstanceData();
            }

            instancedTreeShader.end();
        }
        //context.end();

        if (!chunksToBeRenderedAsGPUheavyImpostors.isEmpty()) {

            impostorShaderGPUheavy.begin(camera,context);
            //impostorShader.begin(camera,context);

            cameraLocation2D.set(camera.position.x,camera.position.z);

            for (int i = 0; i < TREE_TYPES_MAX; i++) {

                LodModel lodModel = lodModels[i];
                //lodModel.texture.bind();

                for (MapChunk mapChunk : chunksToBeRenderedAsGPUheavyImpostors) {
                    float[] data = mapChunk.getTreeTypePositions(i);
                    if (data == null) continue;

                    //lodModel.getImpostor().userData = mapChunk.getTransform(cameraLocation2D,camera.position,lodModel);

                    lodModel.addInstanceData(lodModel.decalIndex,data);
                    //lodModel.renderHeavy(impostorShaderGPUheavy, lodModel.decalIndex, data);
                }

                lodModel.flushInstanceData(lodModel.decalIndex);
            }
            impostorShaderGPUheavy.end();
        }


        if (!chunksToBeRenderedAsImpostors.isEmpty()) {

            impostorShader.begin(camera,context);

            cameraLocation2D.set(camera.position.x,camera.position.z);

            for (int i = 0; i < TREE_TYPES_MAX; i++) {

                LodModel lodModel = lodModels[i];
                lodModel.getImpostor().userData = null;

                for (MapChunk mapChunk : chunksToBeRenderedAsImpostors) {
                        float[] data = mapChunk.getTreeTypePositions(i);
                        if (data == null) continue;

                        if (lodModel.getImpostor().userData == null) {
                            //lodModel.texture.bind();
                            lodModel.getImpostor().userData = mapChunk.getTransform(cameraLocation2D, camera.position, lodModel);
                        }
                        //lodModel.render(impostorShader, lodModel.decalIndex, data);
                        lodModel.addInstanceData(lodModel.optimizedDecalIndex, data);
                    }

                lodModel.flushInstanceData(lodModel.optimizedDecalIndex);
            }
            impostorShader.end();
        }
        context.end();


        batch.begin(camera);

        if (showTerrain)
        {
            for (MapChunk mapChunk : chunksToBeRendered) {
                batch.render(mapChunk.terrainRenderable);
            }

            for (MapChunk chunksToBeRenderedAsImpostor : chunksToBeRenderedAsImpostors) {
                batch.render(chunksToBeRenderedAsImpostor.terrainRenderable);
            }

            for (MapChunk chunksToBeRenderedAsGPUheavyImpostor : chunksToBeRenderedAsGPUheavyImpostors) {
                batch.render(chunksToBeRenderedAsGPUheavyImpostor.terrainRenderable);
            }
        }

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


        treesTotal = 0;
        for (int ii = 0; ii < 3; ii++) {
            lodModel = lodModels[ii];
            font.draw(batch2D,lodModel.ID,10, 84+ii*32);
            for (int i = 0; i < LOD_MAX; i++) {
                font.draw(batch2D, "LOD"+i+": " + lodModel.debugCounters[i] , 140 + (i*220), 84+ii*32);
                treesTotal += lodModel.debugCounters[i];
            }

            if (lodModel.decalIndex > 0) {
                font.draw(batch2D, "DECALS: " + lodModel.debugCounters[lodModel.decalIndex], 140 + (LOD_MAX * 220), 84 + ii * 32);
                treesTotal += lodModel.debugCounters[lodModel.decalIndex];
            }
        }

        font.draw(batch2D,"TREES TOTAL: "+treesTotal,10,30);



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
        return MathUtils.random((float)tileSize);
    }

    private void createWorld(){

        MapChunk.owner = this;

        int worldSizeInTiles = MathUtils.round(maxDistance / tileSize);

        Noise noise = new Noise(1337,1f/64f,Noise.SIMPLEX,1,2.5f,0.5f);

        MapChunk mapChunk;

        int cabinTileX = (MathUtils.random(0,worldSizeInTiles) + MathUtils.random(0,worldSizeInTiles) + MathUtils.random(0,worldSizeInTiles) + MathUtils.random(0,worldSizeInTiles)) / 4;
        int cabinTileY = (MathUtils.random(0,worldSizeInTiles) + MathUtils.random(0,worldSizeInTiles) + MathUtils.random(0,worldSizeInTiles) + MathUtils.random(0,worldSizeInTiles)) / 4;

        int chunkOffsetX,chunkOffsetY;


        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {

                chunkOffsetX = x*tilesPerChunk;
                chunkOffsetY = y*tilesPerChunk;

                mapChunk = world[x][y];
                mapChunk.init(noise,tilesPerChunk,cabinTileX-chunkOffsetX,cabinTileY-chunkOffsetY);
                mapChunk.optimize();

            }
        }


        float ix = cabinTileX * tileSize - worldSizeHalf + 5f;
        float iz = cabinTileY * tileSize - worldSizeHalf + 5f;

        //float[] elevations = new float[4];

        /*
        elevations[0] = terrain.getHeightAtWorldCoord(ix-2.5f,iz-2.5f);
        elevations[1] = terrain.getHeightAtWorldCoord(ix+2.5f,iz-2.5f);
        elevations[2] = terrain.getHeightAtWorldCoord(ix+2.5f,iz+2.5f);
        elevations[3] = terrain.getHeightAtWorldCoord(ix-2.5f,iz+2.5f);


        float cabinElevation = elevations[0];

        for (int i = 1; i < 4; i++) {
            if (elevations[i] > cabinElevation) cabinElevation = elevations[i];
        }

         */

        //float cabinElevation = noise.getConfiguredNoise(cabinTileX / tileSize, cabinTileY / tileSize) * terrainHeightMultiplier + 1f;

        mapChunk = world[cabinTileX / tilesPerChunk][cabinTileY / tilesPerChunk];

        float cabinElevation = mapChunk.getHeightAtWorldCoord(ix,iz);

        cabinPosition = new Vector3(ix , cabinElevation,iz);

        /*
        for (int x = 0; x < worldSizeInTiles; x++) {
            for (int y = 0; y < worldSizeInTiles ; y++) {

                if ((x == cabinTileX) && (y == cabinTileY)) continue;

                mapChunk = world[x / tilesPerChunk][y / tilesPerChunk];

                for (int i = 0; i < treesPerTile; i++) {
                    tt = convertToTreeType(MathUtils.random(100));

                    ix = x * tileSize - worldSizeHalf + getSubPosition();
                    iz = y * tileSize - worldSizeHalf + getSubPosition();

                    mapChunk.addInstance(tt,new Vector3(
                                    ix ,
                                    //noise.getConfiguredNoise((float)x / tilesX, (float)y / tilesY) * 128,
                                    terrain.getHeightAtWorldCoord(ix,iz),
                                    iz),
                            lodModels[tt].radius);
                }
            }
        }

         */


        /*
        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {
                world[x][y].optimize();
            }
        }

         */

        if (showTerrain) {
            ShaderProgram.prependVertexCode = "#version 100\n";
            ShaderProgram.prependFragmentCode = "#version 100\n";

            terrainShader = new DefaultShader(world[0][0].terrainRenderable);
            terrainShader.init();

            for (int x = 0; x < chunksX; x++) {
                for (int y = 0; y < chunksY; y++) {
                    mapChunk = world[x][y];
                    if (mapChunk.terrainRenderable != null)
                        mapChunk.terrainRenderable.shader = terrainShader;

                }
            }
        }


    }

    private void resetCounters(){
        for (int i = 0; i < TREE_TYPES_MAX; i++) {
            lodModels[i].resetCounters();
        }
    }


    private void takeCameraHome()
    {
       // float startPos = chunkSize * (chunksY / 4);
        //camera.position.set(0,128,-startPos);
        camera.position.set(cabinPosition.x,cabinPosition.y + 64,cabinPosition.z-64);
        camera.up.set(Vector3.Y);
        //camera.lookAt(0,0,0);
        camera.lookAt(cabinPosition.x,cabinPosition.y,cabinPosition.z);
        camera.update();
    }

    private void init(boolean use150) {
        instancedShaderProvider = new InstancedShaderProviderGPU(null);
        InstancedShaderProviderGPU.use150 = use150;

        instancedTreeShader = new InstancedShaderProviderGPU.InstancedShader();
        impostorShader = new InstancedShaderProviderGPU.ImpostorShader();
        impostorShaderGPUheavy = new InstancedShaderProviderGPU.ImpostorShaderGPUheavy();
        instancedShaderProvider.impostorShaderGPUheavy = impostorShaderGPUheavy;
        instancedShaderProvider.impostorShader = impostorShader;
        instancedShaderProvider.instancedShader = instancedTreeShader;

        cameraLocation2D = new Vector2();

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
        //takeCameraHome();
        camFrustum = camera.frustum;

        batch2D = new SpriteBatch();

        batch = new ModelBatch(instancedShaderProvider,null);


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

        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {
                world[x][y].dispose();
            }
        }

        if (cabinModel != null) cabinModel.dispose();

        //if (terrain != null) terrain.dispose();

        instancedShaderProvider.dispose();
    }

    public interface DemoEventListener
    {
        public void finished();

        public void working(String message);
    }


}
