package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class TreeChunk {

    public int maxTrees;
    private int counter;
    public TreeInstance[] treeInstances;
    public BoundingBox boundingBox;

    public float worldOffsetX;
    public float worldOffsetY;


    public TreeChunk(int maxTrees, float worldOffsetX, float worldOffsetY) {
        this.maxTrees = maxTrees;
        this.worldOffsetX = worldOffsetX;
        this.worldOffsetY = worldOffsetY;
        this.treeInstances = new TreeInstance[maxTrees];
        //boundingBox = new BoundingBox(new Vector3(-1f,-1f,-1f),new Vector3(1,1,1));
        boundingBox = new BoundingBox();
        counter = 0;
    }

    public void addInstance(int type, Vector3 position, float radius)
    {
        if (counter >= maxTrees) return;
        treeInstances[counter] = new TreeInstance(type,position);
        counter++;
        boundingBox.ext(position,radius);
    }

    public static class TreeInstance
    {
        public int type;
        public Vector3 position;

        public TreeInstance(int type, Vector3 position) {
            this.type = type;
            this.position = position;
        }
    }

}
