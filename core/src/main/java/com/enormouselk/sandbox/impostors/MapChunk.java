package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.BoundingBox;

import java.util.HashMap;

import static com.badlogic.gdx.math.MathUtils.HALF_PI;
import static com.badlogic.gdx.math.MathUtils.round;
import static com.enormouselk.sandbox.impostors.LodModel.MINIMUM_ANGLE_RAD;
import static java.lang.Math.abs;
import static java.lang.Math.min;

public class MapChunk {

    public int maxTrees;
    public float size;

    public ShrinkableFloatArray[] positions;
    public BoundingBox boundingBox;
    public float distanceFromCamera;
    private Vector3 center;

    //todo: add terrain renderable

    public float worldOffsetX;
    public float worldOffsetY;

    private Quaternion q;
    private Matrix4 mat4;

    private DecalTransform decalTransform;



    public MapChunk(int size, int treeTypeCount, int maxTrees, float worldOffsetX, float worldOffsetY) {

        q = new Quaternion();
        mat4 = new Matrix4();
        decalTransform = new DecalTransform();

        this.size = size;
        this.maxTrees = maxTrees;
        this.worldOffsetX = worldOffsetX;
        this.worldOffsetY = worldOffsetY;
        boundingBox = new BoundingBox();
        positions = new ShrinkableFloatArray[treeTypeCount];

        for (int i = 0; i < treeTypeCount; i++) {
            positions[i] = new ShrinkableFloatArray(maxTrees * 3);

        }
    }

    public void addInstance(int type, Vector3 position, float radius)
    {
        //if (positions[type] == null) positions[type] = new ShrinkableFloatArray(maxTrees * 3);
        positions[type].put(position);
        boundingBox.ext(position,radius);
    }

    public void optimize()
    {
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] == null) continue;
            positions[i].shrink();
        }
        center = new Vector3();
        boundingBox.getCenter(center);
    }

    public float[] getTreeTypePositions(int type)
    {
        return positions[type].data;
    }


    public float getDistance(Vector3 from)
    {
        distanceFromCamera = abs(from.dst(boundingBox.max));
        distanceFromCamera = min(abs(from.dst(center)),abs(from.dst(boundingBox.max)));
        distanceFromCamera = min(abs(from.dst(boundingBox.min)),abs(from.dst(boundingBox.max)));
        return distanceFromCamera;
    }

    public DecalTransform getTransform(Vector2 cameraLocation2D, Vector3 cameraPosition, LodModel lodModel)
    {
        float distTemp2D = cameraLocation2D.dst(center.x,center.z);
        float angleTempY = MathUtils.atan2(cameraPosition.y-center.y,distTemp2D);

        float tmpFloat;
        float tmpStepX;
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
            tmpStepX = round((angleTempY - MINIMUM_ANGLE_RAD) / lodModel.angleYStepRad);
            if (tmpStepX >= lodModel.stepsX) tmpStepX = lodModel.stepsX-1;
            tmpFloat =  tmpStepX * lodModel.uvWidth;
        }

        float angleTemp = HALF_PI-MathUtils.atan2(center.z-cameraPosition.z,center.x-cameraPosition.x);

        q.setEulerAnglesRad(angleTemp,angleTempY,0);
        //q.setEulerAnglesRad(0,angleTempY,0);

        // create matrix transform
        mat4.set(Vector3.Zero, q);

        //the actual 3D models have their origin at the bottom-center of the model
        //but decals have their origin in the center, so to make their positions align
        //we need to move the decal a little bit, depending on its rotation
        //if the camera is directly above, there is no need to adjust the position
        //if the camera is looking from at the horizontal plane, we adjust the decal position by width/2
        //and in between we adjust in between

        decalTransform.transform = mat4;
        //decalTransform.moveY = MathUtils.sin(HALF_PI + angleTempY) * lodModel.decalWorldHalfHeight;
        decalTransform.moveY = lodModel.decalWorldHalfHeight;

        decalTransform.uvOffset[0] = tmpFloat;
        //offsets[decalIndex].put(tmpFloat);

        //then we compute the u offset based on the camera angle
        angleTemp = MathUtils.atan2Deg360(cameraPosition.z-center.z,cameraPosition.x - center.x);

        float tmpStepY = round((angleTemp) / lodModel.angleXStep);
        if (tmpStepY >= lodModel.stepsY) tmpStepY = lodModel.stepsY-1;
        tmpFloat =  tmpStepY * lodModel.uvHeight;

        decalTransform.uvOffset[1] = tmpFloat;

        //offsets[LOD_MAX].put(tmpFloat);


        //mat4.translate(0, MathUtils.sin(HALF_PI + angleTempY) * lodModel.decalWorldHalfHeight, 0);

        //mat4.trn(0, 50, 0);
        return decalTransform;
    }

    public class DecalTransform
    {
        float moveY;
        Matrix4 transform;
        float[] uvOffset;

        public DecalTransform() {
            transform = new Matrix4();
            //uvOffset = new Vector2();
            uvOffset = new float[2];
        }
    }


}
