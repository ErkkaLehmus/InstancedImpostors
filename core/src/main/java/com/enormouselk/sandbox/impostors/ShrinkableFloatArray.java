package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.math.Vector3;

import java.util.Arrays;

/**
 * A custom class to replace FloatBuffer
 * Surely this could be optimized ?
 */
public class ShrinkableFloatArray {
    public float[] data;
    public int capacity;
    public int position;



    public ShrinkableFloatArray(int capacity) {
        this.capacity = capacity;
        data = new float[capacity];
    }

    public void position(int newPos) {
        this.position = newPos;
    }

    public boolean isEmpty()
    {
        return position == 0;
    }

    public void put(float val) {

        if (position >= capacity) return;

        data[position] = val;
        position++;
    }

    public void put(float val1, float val2, float val3) {
        put(val1);
        put(val2);
        put(val3);
    }

    public void put(Vector3 values) {
        put(values.x);
        put(values.y);
        put(values.z);
    }

    public void put(float[] values) {
        for (float value : values) {
            put(value);
        }
    }

    public void clear() {
        position = 0;
    }

    public void shrink()
    {
        if (position >= capacity) return;
        if (position == 0)
            this.data = null;
        else
            this.data = Arrays.copyOf(data, position);
    }


}
