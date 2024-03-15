/*******************************************************************************
 *
 * License: You can freely copy, use, extend and modidy this code or parts of
 * this code in your own projects, both non-commercial and commercial.
 * You don't need to give credits to me, nor to replicate this license text.
 * Erkka Lehmus, 14th of March 2024
 *
 * The software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.
 *
 ******************************************************************************/

package com.enormouselk.sandbox.impostors;

import static com.badlogic.gdx.math.MathUtils.HALF_PI;
import static com.badlogic.gdx.math.MathUtils.cosDeg;
import static com.badlogic.gdx.math.MathUtils.round;
import static java.lang.Math.pow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.BufferUtils;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneModel;

//import java.nio.Buffer;
//import java.nio.FloatBuffer;
import java.nio.IntBuffer;
//import java.util.Arrays;

class LodModel {

    private static final String debugFilePath = null;
    //private static final String debugFilePath = "tmp/lodtest";
    //if debugFilePath is not null, png files of generated impostors will be saved at the given folder

    private float textureSize;
    private final float decalCameraDistance = 80;
    //the above 2 values affect the quality of decals
    //the higher the texureSize the better the image quality
    //the smaller the camera distance the better the image quality
    //your model size determines what camera distance you can use,
    //the taller the models the further away the camera needs to be.
    //currently trial and error is the only way to find out the optimal values to use
    //so a possible future improvement would be to write an algorithm to estimate the values


    public static final int DECAL_INSTANCE_DATA_SIZE = 18;
    //for the demo we store for each instance
    // - world transformation : 4 * 4 = 16 floats
    // - UV offset : 2 floats
    // this makes 16 floats of data per instance
    // I'm not sure but I have a feeling that the world transformation could be optimized to be smaller
    // but that will take some more learning, a lot of testing and failures, and maybe eventually realizing
    // why it needs to be 16 floats and no less

    private int decalIndex;
    private float[] LODdistances;

    private boolean hasDecal;


    public InstancedShaderProvider instancedShaderProvider;
    public String ID;
    public Texture texture;
    public Renderable[] renderables;

    //public FloatBuffer[] offsets;
    public LotsOfFloats[] offsets;
    public int decalCount;
    public int[] lodCount;

    public float radius;


    public float uvWidth;
    public float uvHeight;
    public float angleYStep;
    public float angleYStepRad;
    public float angleXStep;
    public float decalWorldDepth;
    public float decalWorldHeight;
    public float decalWorldHalfHeight;
    public float decalWorldYoffset;
    int stepsY;
    int stepsX;

    int tmpStepX;
    int tmpStepY;
    float tmpFloat;

    int LOD_MAX;
    int maxDecalInstances;
    int maxModelInstances;

    private Quaternion q;
    private Matrix4 mat4;
    private float distTemp;
    private float distTemp2D;
    private int lodTemp;
    float angleTemp;
    float angleTempY;

    private static final float MINIMUM_ANGLE_RAD = (float) (Math.PI / 6f);

    private final static Material blankMaterial = new Material();


    /**
     * Construct a 3D model with different LOD levels and an 2D impostor. The demo doesn't support glb but has a minimal shader using vertex colors and normals.
     * @param filename currently expecting .glb files named as mymodel-lod0.glb, mymodel-lod1.glb, mymodel-lod2.glb, IMPORTANT: for the parameter value give only "mymodel", that will be used as a basis to generate the actual filenames to load from
     * @param ID an optional string to identify this model. Currently this does nothing.
     * @param lodMax 3. This is groundwork for making the number of lod levels customizable for each models. But the demo just assumed 3 for each model.
     * @param maxInstances maximum amount if instances that can be displayed at once
     * @param decalDistance in range 0.0 - 1.0
     * @param maxDistance to determine the actual thresholds for each lod level. This would be either camera.far or the world size. If decalDistance is 0.5 and maxDistance is 1000, models will be displayed as decals (impostors) at 500m and further
     * @param environment environment
     * @param shaderProvider shaderProvider
     */
    public LodModel(String filename, String ID, int lodMax, boolean generateImpostor, int maxInstances, float decalDistance, float maxDistance, int textureSize, Environment environment, InstancedShaderProvider shaderProvider) {
        this.ID = ID;
        this.instancedShaderProvider = shaderProvider;
        LOD_MAX = lodMax;

        this.hasDecal = generateImpostor;
        if (generateImpostor) {
            decalIndex = LOD_MAX;
            int maxTextureSize = getMaxTextureSize();
            if (textureSize > maxTextureSize) textureSize = maxTextureSize;
            this.textureSize = textureSize;
            this.maxDecalInstances = maxInstances;
            maxModelInstances = maxInstances;
            //below is what I'd actually like to have, but that will need more testing to ensure it works reliably
            //maxModelInstances = MathUtils.ceilPositive(maxInstances * decalDistance);
        }
        else {
            decalIndex = -1;
            this.textureSize = 0;
            this.maxDecalInstances = 0;
            maxModelInstances = maxInstances;
        }

        initLodDistances(decalDistance * maxDistance);



        lodCount = new int[LOD_MAX];
        offsets = new LotsOfFloats[LOD_MAX + 1];
        q = new Quaternion();
        mat4 = new Matrix4();
        renderables = new Renderable[LOD_MAX + 1];
        setupInstancedMeshes(filename,environment);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
        for (int i = 0; i < renderables.length; i++) {
            if (renderables[i] != null) {
                renderables[i].shader.dispose();
                renderables[i].meshPart.mesh.dispose();
            }
            //if (offsets[i] != null) offsets[i].clear();
        }
    }

    public void initLodDistances(float decalDistance)
    {
        LODdistances = new float[LOD_MAX];

        float dist = decalDistance;

        if (hasDecal) {
            for (int i = decalIndex - 1; i >= 0; i--) {
                LODdistances[i] = dist;
                dist = dist / 2f;
            }
        }
        else
        {
            for (int i = LOD_MAX-2; i >= 0; i--) {
                LODdistances[i] = dist;
                dist = dist / 2f;
            }
        }
    }

    public void reallocBuffers(float decalDistance)
    {
        int oldMax = maxModelInstances;
        maxModelInstances = MathUtils.ceilPositive(maxDecalInstances * decalDistance);

        if (maxModelInstances > oldMax)
        {
            for (int i = 0; i < decalIndex; i++) {
                offsets[i].grow(maxModelInstances * 3);
                //offsets[i] = new LotsOfFloats(maxModelInstances * 3); // 16 floats for mat4
            }
        }


    }

    public int getDecalDistance()
    {
        if (hasDecal) return (int) LODdistances[decalIndex-1];
        return -1;
    }

    /**
     * This must be called before updating the instance data
     */
    public void resetInstanceData() {
        for (int i = 0; i < LOD_MAX; i++) {
            lodCount[i] = 0;
            offsets[i].position(0);
        }

        decalCount = 0;
        if (hasDecal)
            offsets[decalIndex].position(0);
    }

    /**
     * This does the actual updating, calculating the LOD level to use, and determining the impostor image based on camera angle
     * @param cameraPosition x,y,z
     * @param cameraLocation2D x,z
     * @param instancePosition x,y,z
     */
    public void updateInstanceData(Vector3 cameraPosition, Vector2 cameraLocation2D, Vector3 instancePosition)
    {
        distTemp = cameraPosition.dst(instancePosition);
        lodTemp = getLODlevel(distTemp);
        //get the LOD level based on distance between the camera and the instance

        if (lodTemp == decalIndex)
        {
            //this instance is so far away that we use the impostor instead of actual 3D

            //this part of the code could use some more cleaning up
            //also I'd guess there is still room for optimization.
            //I tried to use setToLookAt() but didn't (yet) figure out exactly how to do it
            //so I just manually calculate the needed rotations to make the decal always face towards the camera

            distTemp2D = cameraLocation2D.dst(instancePosition.x,instancePosition.z);
            angleTempY = MathUtils.atan2(cameraPosition.y-instancePosition.y,distTemp2D);
            //angleTempY = (float) Math.atan2(cameraPosition.y-instancePosition.y,distTemp2D);

            //angleTempY = angleTempY* MathUtils.radiansToDegrees;

            if (angleTempY > HALF_PI)
                angleTempY = HALF_PI - (angleTempY - HALF_PI);
            if (angleTempY < MINIMUM_ANGLE_RAD) {
                tmpFloat = 0;
                tmpStepX = 0;
            }
            else
            {
                tmpStepX = round((angleTempY - MINIMUM_ANGLE_RAD) / angleYStepRad);
                if (tmpStepX >= stepsX) tmpStepX = stepsX-1;
                tmpFloat =  tmpStepX * uvWidth;
            }

            angleTemp = HALF_PI-MathUtils.atan2(instancePosition.z-cameraPosition.z,instancePosition.x-cameraPosition.x);
            q.setEulerAnglesRad(angleTemp,angleTempY,0);
            //q.setEulerAnglesRad(0,angleTempY,0);

            // create matrix transform
            mat4.set(instancePosition, q);

            //the actual 3D models have their origin at the bottom-center of the model
            //but decals have their origin in the center, so to make their positions align
            //we need to move the decal a little bit, depending on it's rotation
            //if the camera is directly above, there is no need to adjust the position
            //if the camera is looking from at the horizontal plane, we adjust the decal position by width/2
            //and in between we adjust in between

            mat4.translate(0, MathUtils.sin(HALF_PI + angleTempY) * decalWorldHalfHeight, 0);


            // put the 16 floats for mat4 in the float buffer
            offsets[decalIndex].put(mat4.getValues());

            //store the v offset for uv offset
            offsets[decalIndex].put(tmpFloat);

            //then we compute the u offset based on the camera angle
            angleTemp = MathUtils.atan2Deg360(cameraPosition.z-instancePosition.z,cameraPosition.x - instancePosition.x);

            tmpStepY = round((angleTemp) / angleXStep);
            if (tmpStepY >= stepsY) tmpStepY = stepsY-1;
            tmpFloat =  tmpStepY * uvHeight;
            offsets[LOD_MAX].put(tmpFloat);

            decalCount++;

        }
        else
        {
            //use the chosen LOD
            offsets[lodTemp].put(instancePosition.x);
            offsets[lodTemp].put(instancePosition.y);
            offsets[lodTemp].put(instancePosition.z);
            lodCount[lodTemp]++;
        }
    }

    private int getLODlevel(float distance)
    {
        if (hasDecal) {
            for (int i = decalIndex - 1; i >= 0; i--) {
                if (distance > LODdistances[i]) return i + 1;
            }
        }
        else
        {
            for (int i = LOD_MAX-2; i>=0; i--) {
                if (distance > LODdistances[i]) return i+1;
            }
            return 0;
        }
        return 0;
    }


    /**
     * Use this after updating the instance data, before rendering
     */
    public void pushInstanceData()
    {
        if (hasDecal) {
            if (decalCount > 0) {
                renderables[decalIndex].meshPart.mesh.setInstanceData(offsets[decalIndex].data, 0, offsets[decalIndex].position);

                //this was the old buffer-based approach
            /*
            ((Buffer) offsets[decalIndex]).position(0);
            renderables[decalIndex].meshPart.mesh.setInstanceData(offsets[decalIndex], decalCount*DECAL_INSTANCE_DATA_SIZE);
             */
            }
        }

        for (int i = 0; i < LOD_MAX ; i++) {
            if (lodCount[i] > 0)
            {
                renderables[i].meshPart.mesh.setInstanceData(offsets[i].data,0, offsets[i].position);
                /*
                ((Buffer) offsets[i]).position(0);
                renderables[i].meshPart.mesh.setInstanceData(offsets[i], lodCount[i] * 3);
                 */
            }
        }
    }

    /**
     * Render all the 3D instances and impostors using the given batch
     * @param batch quick, to the batchmobile!
     */
    public void render(ModelBatch batch)
    {
        for (int i = 0; i < LOD_MAX; i++) {
            if (lodCount[i] > 0) {
                batch.render(renderables[i]);
            }
        }

        if (hasDecal) {
            if (decalCount > 0) {
                texture.bind(0);
                batch.render(getImpostor());
            }
        }
    }

    public Renderable getImpostor()
    {
        return renderables[decalIndex];
    }


    private void setupInstancedMeshes(String modelFile, Environment environment) {
        Mesh lod0 = loadFromGLB(modelFile + "-lod0.glb");
        setupInstancedMesh(lod0, 0, environment);
        radius = lod0.calculateRadius(0,0,0);

        Mesh lod1 = loadFromGLB(modelFile + "-lod1.glb");
        setupInstancedMesh(lod1, 1, environment);

        Mesh lod2 = loadFromGLB(modelFile + "-lod2.glb");
        setupInstancedMesh(lod2, 2, environment);

        if (hasDecal) setupInstancedDecals(2);
    }

    private Mesh loadFromGLB(String filename) {
        SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal(filename));

        Model model = sceneAsset.scene.model;
        Mesh mesh = model.meshes.get(0);

        //This is something I'm not 100% sure about, but what little I understand
        //the .glb format stores colors in linear space
        //but my simple shaders want them in the good old rgb
        //so I do the conversion beforehand to keep the shader happy!
        //also, to optimize memory usage I'd guess here we could also just pack the color
        //but for the demo I left it this way, for these are things I still need to learn more about

        int coff = mesh.getVertexAttributes().getOffset(VertexAttributes.Usage.ColorUnpacked);
        if (coff >= 0) {
            int counter = coff;
            int stride = mesh.getVertexSize() / 4;
            int max = mesh.getNumVertices() * stride;

            float[] vertData = new float[max];
            vertData = mesh.getVertices(vertData);

            while (counter < max) {
                vertData[counter] = (float) pow(vertData[counter], 1.0 / 2.2);
                //vertData[counter] = 1f;
                vertData[counter + 1] = (float) pow(vertData[counter + 1], 1.0 / 2.2);
                vertData[counter + 2] = (float) pow(vertData[counter + 2], 1.0 / 2.2);

                counter += stride;
            }

            mesh.setVertices(vertData);
        }

        //we dispose the rest of the sceneAsset. Mesh will be disposed in the LodModel.dispose()
        if (sceneAsset.scenes != null) {
            for (SceneModel scene : sceneAsset.scenes) {
                scene.dispose();
            }
        }
        if (sceneAsset.textures != null) {
            for (Texture texture : sceneAsset.textures) {
                texture.dispose();
            }
        }

        return mesh;
    }


    //helper functions to scan if a given line has an actual pixel
    private boolean scanVerticalLine(Pixmap pixmap, int scanX) {
        int pixmapheight = pixmap.getHeight();
        for (int y = 0; y < pixmapheight; y++) {
            if (pixmap.getPixel(scanX, y) != 0) return true;
        }
        return false;
    }
    private boolean scanHorizontalLine(Pixmap pixmap, int scanY) {
        int pixmapwidht = pixmap.getWidth();
        for (int x = 0; x < pixmapwidht; x++) {
            if (pixmap.getPixel(x, scanY) != 0) return true;
        }
        return false;
    }

    //another helper function processing the given pixmap, finding either the first empty or non-empty line at given orientation
    private int getFirstLine(Pixmap pixmap, boolean nonEmpty, int start, boolean horizontal, int step) {
        boolean loopMe = true;
        int max;
        if (horizontal) {
            max = pixmap.getWidth();
            while (loopMe) {
                if (scanHorizontalLine(pixmap, start) == nonEmpty) return start;
                start += step;
                if (start < 0) loopMe = false;
                if (start >= max) loopMe = false;
            }
            return start;
        }

        max = pixmap.getHeight();
        while (loopMe) {
            if (scanVerticalLine(pixmap, start) == nonEmpty) return start;
            start += step;
            if (start < 0) loopMe = false;
            if (start >= max) loopMe = false;
        }
        return start;
    }

    //a helper function to take a snapshot of the renderable, and clip the image to the pixmap
    private void clipDecal(ModelBatch modelBatch, Camera tmpCamera, Renderable renderable, Pixmap fboPixmap, Pixmap clippedPixmap, int cropX, int cropY, int pxWidth, int pxHeight, int offsetX, int offsetY) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(tmpCamera);
        modelBatch.render(renderable);
        modelBatch.end();

        fboPixmap = Pixmap.createFromFrameBuffer(0, 0, fboPixmap.getWidth(), fboPixmap.getHeight());
        clippedPixmap.drawPixmap(fboPixmap, offsetX, offsetY, cropX, cropY, pxWidth, pxHeight);
    }

    /**
     * Generate the impostor decal. The 3D LOD versions must be generated before calling this.
     * This places the 3D model in the middle of the world, and places camera at a hardcoded distance looking at the model
     * A snapshot picture is taken and stored in a pixmap. Then camera is rotated a little, and another picture is taken,
     * and so on, until we have a texure full of images of the model seen from different angles.
     * @param fromIndex the LOD level to use as a basis for generating the impostor, typically you'd want this to be the lowest-level LOD
     */
    private void setupInstancedDecals(int fromIndex) {

        int fboWidth = (int) textureSize;
        //int fboHeight = round((float) Gdx.graphics.getHeight() / Gdx.graphics.getWidth() * textureSize);
        int fboHeight = (int) (textureSize / 16f * 9f);

        int offsetX = 0;
        int offsetY = 0;

        float camDistance = decalCameraDistance;

        //we start by looking at the model from the camera distance, at 15 degrees elevation angle
        PerspectiveCamera tmpCamera = new PerspectiveCamera(67, fboWidth, fboHeight);
        tmpCamera.near = 0.1f;
        tmpCamera.far = decalCameraDistance*2;
        tmpCamera.position.set(0, camDistance / 4, camDistance);
        tmpCamera.up.set(Vector3.Y);
        tmpCamera.lookAt(0, 0, 0);
        tmpCamera.update();

        Vector3 instanceLocation = new Vector3(0, 0, 0);
        Renderable renderable = renderables[fromIndex];

        offsets[fromIndex].position(0);
        //((Buffer) offsets[fromIndex]).position(0);

        offsets[fromIndex].put(0);
        offsets[fromIndex].put(0);
        offsets[fromIndex].put(0);

        //((Buffer) offsets[fromIndex]).position(0);

        renderable.meshPart.mesh.setInstanceData(offsets[fromIndex].data,0, 3);
        //renderable.meshPart.mesh.setInstanceData(offsets[fromIndex], 3);

        ModelBatch modelBatch = new ModelBatch(instancedShaderProvider);

        FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, true);
        fbo.begin();

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        //((Buffer) offsets[fromIndex]).position(0);
        modelBatch.begin(tmpCamera);
        modelBatch.render(renderable);
        modelBatch.end();


        Pixmap fboPixmap = Pixmap.createFromFrameBuffer(0, 0, fboWidth, fboHeight);

        if (debugFilePath != null) {
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child("fbo.png"), fboPixmap);
        }

        int cropX = getFirstLine(fboPixmap, false, fboWidth / 2, false, -1);
        int cropX2 = getFirstLine(fboPixmap, false, fboWidth / 2, false, 1);
        int cropY = getFirstLine(fboPixmap, false, fboHeight / 2, true, -1);
        int cropY2 = getFirstLine(fboPixmap, true, fboHeight, true, -1);

        int pxWidth = cropX2 - cropX;
        int pxHeight = cropY2 - cropY;

        uvWidth = (float) pxWidth / textureSize;
        uvHeight = (float) pxHeight / textureSize;

        //For the demo we generate images of the model seen from E,NE,N,NW,W,SW,S,SE - that makes 8 different camera angles
        //this, I think is the minimum, and since I had trouble fitting all the images on a 1024 x 1024 texture,
        //in my demo this value can't be any smaller nor any higher
        stepsY = 8;

        //well, but for taking the camera higher and higher, we have more storage space in that direction
        //so we compute how many images we can pack side by side
        //camera angles 0 - 30 use the same image of the object seen directly from the side
        //so we need 60 degrees rotation to get above the model, and we generate images for those
        stepsX = (int) textureSize / pxWidth;
        angleYStep = 60f / (stepsX-1);
        angleYStepRad = angleYStep * MathUtils.degreesToRadians;
        offsetY = 0;

        Pixmap clippedPixmap = new Pixmap((int) textureSize, (int) textureSize, Pixmap.Format.RGBA8888);

        float angleX = 270;
        angleXStep = 360f / stepsY;

        for (int dir = 0; dir < stepsY; dir++) {

            offsetX = 0;
            float angleY = 15;
            for (int i = 0; i < stepsX; i++) {

                float cose = cosDeg(angleY);

                Vector3 camPos = new Vector3(0, 0, camDistance);
                Quaternion quaternion = new Quaternion();
                quaternion.setEulerAngles(angleX, angleY, 0);

                camPos.mul(quaternion);

                tmpCamera.position.set(camPos);
                tmpCamera.up.set(Vector3.Y);

                tmpCamera.lookAt(instanceLocation);
                tmpCamera.update();

                //when the camera is taken higher, we need to gently adjust the image position,
                //otherwise when seen from directly above we would only see a northern half of the model
                //and the rest being cut out.
                int dropY = round(cose * pxWidth - pxWidth);

                //after the camera has been rotated we take a snapshot and store it in the pixmap
                clipDecal(modelBatch, tmpCamera, renderable, fboPixmap, clippedPixmap, cropX, cropY + dropY, pxWidth, pxHeight, offsetX, offsetY - (dropY));
                if (angleY == 15) angleY = 30;
                angleY += angleYStep;
                offsetX += pxWidth;
            }

            offsetY += pxHeight;
            angleX += angleXStep;
            if (angleX >= 360) angleX -= 360;
        }

        //if we have the path, we store the generated pixmap there
        if (debugFilePath != null) {
            String saveName = "test_" + ID + ".png";
            PixmapIO.writePNG(Gdx.files.external(debugFilePath).child(saveName), clippedPixmap);
        }

        fboPixmap.dispose();

        //OK, I'm not 100% sure what would be the best configuration for mipmaps and filters.
        //Feel free to experiment to find an optimal solution!
        texture = new Texture(clippedPixmap, true);
        //texture.setFilter(Texture.TextureFilter.MipMapNearestNearest, Texture.TextureFilter.Linear);
        //texture = new Texture(clippedPixmap, false);
        //texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        fbo.end();

        modelBatch.dispose();
        fbo.dispose();
        clippedPixmap.dispose();

        stepsX--;


        //OK, we now have our texture, then we move on to generate the actual decal


        final int VERTEX_SIZE = 3 + 2;
        //3 floats for xyz position, 2 floats for UV coordinates
        final int SIZE = 4 * VERTEX_SIZE;

        float[] vertices = new float[SIZE];
        short[] meshindices = new short[6];

        meshindices[0] = 3;
        meshindices[1] = 1;
        meshindices[2] = 0;
        meshindices[3] = 3;
        meshindices[4] = 2;
        meshindices[5] = 1;

        Mesh mesh = new Mesh(true, 4, 6,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoords0")
        );

        //and just like that, now we have a flat decal, we will use it as a billboard to make an impostor, yay!


        //we need to know the world size of the actual model
        BoundingBox boundingBox = new BoundingBox();
        renderable.meshPart.mesh.calculateBoundingBox(boundingBox);

        int offset;

        int decalWorldWidth = round(boundingBox.getWidth());
        decalWorldHeight = boundingBox.getHeight();
        decalWorldHalfHeight = decalWorldHeight / 2;
        decalWorldDepth = boundingBox.getDepth();
        decalWorldYoffset = boundingBox.getHeight() / 2;

        float x = 0.5f;
        float z = 0.5f;
        float u = uvWidth;
        float v = uvHeight;

        for (int i = 0; i < 4; i++) {
            offset = i * VERTEX_SIZE;

            if (i == 1) {
                x = 0.5f;
                z = -0.5f;
                u = uvWidth;
                v = 0;
            }

            if (i == 2) {
                x = -0.5f;
                z = -0.5f;
                u = 0f;
                v = 0;
            }

            if (i == 3) {
                x = -0.5f;
                z = 0.5f;
                u = 0;
                v = uvHeight;
            }

            vertices[offset] = x * decalWorldWidth;
            vertices[offset + 1] = z * decalWorldHeight;
            vertices[offset + 2] = 0;
            vertices[offset + 3] = u;
            vertices[offset + 4] = v;

        }

        mesh.setVertices(vertices, 0, SIZE);
        mesh.setIndices(meshindices);

        //OK we have our mesh, now we enable instancing so that we can draw lots of them!

        mesh.enableInstancedRendering(true, maxDecalInstances,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "i_uvOffset"));


        // Create offset FloatBuffer that will hold matrix4 and uv offset for each instance to pass to shader
        offsets[decalIndex] = new LotsOfFloats(maxDecalInstances * DECAL_INSTANCE_DATA_SIZE); // 16 floats for mat4
        //offsets[decalIndex] = BufferUtils.newFloatBuffer(maxDecalInstances * DECAL_INSTANCE_DATA_SIZE); // 16 floats for mat4

        //((Buffer) offsets[decalIndex]).position(0);
        //mesh.setInstanceData(offsets[decalIndex]);

        int indices = mesh.getNumIndices();


        //finally, we can make a renderable out of our instanced mesh!
        Renderable decalRenderable = new Renderable();
        decalRenderable.meshPart.set("DECAL", mesh, 0, indices, GL20.GL_TRIANGLES);
        decalRenderable.environment = renderable.environment;
        decalRenderable.worldTransform.idt();
        decalRenderable.shader = instancedShaderProvider.createDecalShader(decalRenderable);
        decalRenderable.shader.init();
        TextureAttribute attr = new TextureAttribute(TextureAttribute.Diffuse, texture);
        decalRenderable.material = blankMaterial;
        //decalRenderable.material = new Material(attr);

        renderables[decalIndex] = decalRenderable;
    }


    private void setupInstancedMesh(Mesh mesh, int lodIndex, Environment environment) {


        //OK, here comes one of the bottlenecks with instanced rendering - when enabling the instanced rendering
        //we need to define maximum amount of instances. And I didn't yet find a way to dynamically increase
        //that maximum on the fly. But for the demo I wanted a last-minute addition; dynamically adjusting
        //the LOD distances when running the demo. So, to make that possible, instead of capping the
        //instance count I set it to maximum (and then just typically using only some 20 - 30% of the capacity
        mesh.enableInstancedRendering(true, maxModelInstances,
            new VertexAttribute(VertexAttributes.Usage.Generic, 3, "i_worldTrans"));

        /*
        mesh.enableInstancedRendering(true, maxModelInstances,
            new VertexAttribute(VertexAttributes.Usage.Generic, 3, "i_worldTrans"));
         */

        offsets[lodIndex] = new LotsOfFloats(maxModelInstances * 3); // 16 floats for mat4
        //offsets[lodIndex] = BufferUtils.newFloatBuffer(maxModelInstances * 3); // 16 floats for mat4

        //((Buffer) offsets[lodIndex]).position(0);
        //mesh.setInstanceData(offsets[lodIndex]);

        int indices = mesh.getNumIndices();

        Renderable renderable = new Renderable();
        renderable.meshPart.set("Tree", mesh, 0, indices, GL20.GL_TRIANGLES);
        renderable.environment = environment;
        renderable.worldTransform.idt();
        renderable.shader = instancedShaderProvider.createShader(renderable);
        renderable.shader.init();

        //for the demo the actual 3D models are very simple, so we just assign an empty material for them
        //the shader will anyway use the vertex colors instead of a material
        renderable.material = blankMaterial;
        renderables[lodIndex] = renderable;
    }

    private static int getMaxTextureSize () {
        IntBuffer buffer = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buffer);
        return buffer.get(0);
    }

    /**
     * A custom class to replace FloatBuffer
     * Surely this could be optimized ?
     */
    private static class LotsOfFloats
    {
        public float[] data;
        public int capacity;
        public int position;

        public LotsOfFloats(int capacity) {
            this.capacity = capacity;
            data = new float[capacity];
        }

        public void position(int newPos)
        {
            this.position = newPos;
        }

        public void put(float val)
        {
            data[position] = val;
            position++;
        }

        public void put(float[] values)
        {
            for (float value : values) {
                put(value);
            }
        }

        /**
         * Dynamically grow capacity, use with caution!
         * @param newCapacity new capacity
         */
        public void grow(int newCapacity)
        {
            if (newCapacity <= capacity) return;
            float[] newData = new float[newCapacity];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
            capacity = newCapacity;
        }

        public void clear()
        {
            position = 0;
        }

    }

}
