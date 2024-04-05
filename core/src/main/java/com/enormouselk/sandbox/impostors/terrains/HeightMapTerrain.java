package com.enormouselk.sandbox.impostors.terrains;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;

import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector2;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

public class HeightMapTerrain extends Terrain {
    private static final Vector3 c00 = new Vector3();
    private static final Vector3 c01 = new Vector3();
    private static final Vector3 c10 = new Vector3();
    private static final Vector3 c11 = new Vector3();

    private final HeightField field;

    public HeightMapTerrain(float[] data, int width,  float magnitude) {
        this.size = width*10;
        this.width = width;
        this.heightMagnitude = magnitude;

        field = new HeightField(true,data,width,width,true,
                VertexAttributes.Usage.Position
                | VertexAttributes.Usage.Normal
                | VertexAttributes.Usage.TextureCoordinates);


        field.corner00.set(0, 0, 0);
        field.corner10.set(size, 0, 0);
        field.corner01.set(0, 0, size);
        field.corner11.set(size, 0, size);
        field.magnitude.set(0f, magnitude, 0f);
        field.update();

        texture = new Texture(Gdx.files.internal("graphics/moss.png"), true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        //TextureAttribute textureAttribute = TextureAttribute.createDiffuse(texture);
        //textureAttribute.scaleU = 50f;
        //textureAttribute.scaleV = 50f;

        ground = new Renderable();
        Environment environment = new Environment();
        
        DirectionalLight light = new DirectionalLight();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);

        environment.add(light);
        environment.set(new ColorAttribute(ColorAttribute.Fog, 0.8f, 0.8f, 1f, 1f));

        Material material = new Material("TERRAIN_0",TextureAttribute.createDiffuse(texture));



        ground.worldTransform.setTranslation(-(float)size/2f,0,-(float)size/2f);
        ground.meshPart.mesh = field.mesh;

        //ground.meshPart.mesh.transform(new Matrix4(new Vector3(-(float)size/2f,0,-(float)size/2f),new Quaternion(),new Vector3(1f,1f,1f)));

        ground.meshPart.primitiveType = GL20.GL_TRIANGLES;
        ground.meshPart.offset = 0;
        ground.meshPart.size = field.mesh.getNumIndices();
        ground.environment = environment;
        ground.material = material;
        ground.meshPart.update();




        //ground.material = new Material(textureAttribute);

        /*
        PBRTextureAttribute PbrTextureAttribute =
                PBRTextureAttribute.createBaseColorTexture(texture);
        textureAttribute.scaleU = 50f;
        textureAttribute.scaleV = 50f;

         */


        //material.set(PbrTextureAttribute);

        /*
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.part("terrain", field.mesh, GL20.GL_TRIANGLES, material);
        groundModelInstance = new ModelInstance(mb.end());

         */
    }

    @Override
    public float getHeightAtWorldCoord(float worldX, float worldZ) {
        // Convert world coordinates to a position relative to the terrain
        ground.worldTransform.getTranslation(c00);

        float terrainX = worldX - c00.x;
        float terrainZ = worldZ - c00.z;

        // The size between the vertices
        float gridSquareSize = size / ((float) width - 1);

        // Determine which grid square the coordinates are in
        int gridX = (int) Math.floor(terrainX / gridSquareSize);
        int gridZ = (int) Math.floor(terrainZ / gridSquareSize);

        // Validates the grid square
        if (gridX >= width - 1 || gridZ >= width - 1 || gridX < 0 || gridZ < 0) {
            return 0;
        }

        // Determine where on the grid square the coordinates are
        float xCoord = (terrainX % gridSquareSize) / gridSquareSize;
        float zCoord = (terrainZ % gridSquareSize) / gridSquareSize;

        // Determine the triangle we are on and apply barrycentric.
        float height;
        if (xCoord <= (1 - zCoord)) { // Upper left triangle
            height =
                    barryCentric(
                            c00.set(0, field.data[gridZ * width + gridX], 0),
                            c10.set(1, field.data[gridZ * width + (gridX + 1)], 0),
                            c01.set(0, field.data[(gridZ + 1) * width + gridX], 1),
                            new Vector2(xCoord, zCoord));
        } else {
            height =
                    barryCentric(
                            c10.set(1, field.data[gridZ * width + (gridX + 1)], 0),
                            c11.set(1, field.data[(gridZ + 1) * width + (gridX + 1)], 1),
                            c01.set(0, field.data[(gridZ + 1) * width + gridX], 1),
                            new Vector2(xCoord, zCoord));
        }

        return height * heightMagnitude;
    }

    public static float barryCentric(Vector3 p1, Vector3 p2, Vector3 p3, Vector2 pos) {
        float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
        float l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
        float l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
        float l3 = 1.0f - l1 - l2;
        return l1 * p1.y + l2 * p2.y + l3 * p3.y;
    }

    @Override
    public void dispose() {
        field.dispose();
    }
}
