package com.enormouselk.sandbox.impostors.terrains;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.utils.Disposable;

public abstract class Terrain implements Disposable {
    /** A value we can set to change the actual size of the terrain * */
    protected int size;

    /**
     * Represents the width in vertices, for heightmaps this would be the height map image width *
     */
    protected int width;

    /**
     * A height scaling factor since heightmap values and noise values are generally between 0.0 -
     * 1.0 *
     */
    protected float heightMagnitude;

    protected Renderable ground;

    public Renderable getRenderable() {
        return ground;
    }
    
    
    protected ModelInstance groundModelInstance;

    public ModelInstance getModelInstance() {
        return groundModelInstance;
    }
    

    public abstract float getHeightAtWorldCoord(float worldX, float worldZ);
}
