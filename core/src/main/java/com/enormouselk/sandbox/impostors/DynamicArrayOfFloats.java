package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.math.Vector3;

import static com.badlogic.gdx.math.MathUtils.round;

/**
 * A custom class to replace FloatBuffer
 * Surely this could be optimized ?
 */
class DynamicArrayOfFloats {
    public float[] data;
    public int capacity;
    public int position;

    public DynamicArrayOfFloats(int capacity) {
        this.capacity = capacity;
        data = new float[capacity];
    }

    public void position(int newPos) {
        this.position = newPos;
    }

    public void put(float val) {
        data[position] = val;
        position++;
    }

    public void put(float val1, float val2, float val3) {
        put(val1);
        put(val2);
        put(val3);
    }

    public void put(float[] values) {
        for (float value : values) {
            put(value);
        }
    }

    public void safePut(float val) {
        if (position >= capacity) {
            grow(round(capacity * 1.25f));
        }
        data[position] = val;
        position++;
    }

    public void safePut(float val1, float val2, float val3) {
        if (position + 3 >= capacity) {
            grow(round(capacity * 1.25f));
        }
        data[position] = val1;
        position++;
        data[position] = val2;
        position++;
        data[position] = val3;
        position++;
    }

    public void safePut(Vector3 values) {
        if (position + 3 >= capacity) {
            grow(round(capacity * 1.25f));
        }
        data[position] = values.x;
        position++;
        data[position] = values.y;
        position++;
        data[position] = values.z;
        position++;
    }

    public void safePut(float[] values) {
        if (position + values.length >= capacity) {
            grow(round(capacity * 1.25f));
        }
        for (float value : values) {
            put(value);
        }
    }

    /**
     * Dynamically grow capacity, keeping current content.
     *
     * @param newCapacity new capacity
     */
    public void grow(int newCapacity) {
        if (newCapacity <= capacity) return;
        float[] newData = new float[newCapacity];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
        capacity = newCapacity;
    }

    /**
     * Grow capacity, destroying content
     * Use only for an empty container!
     *
     * @param newCapacity new capacity
     */
    public void setCapacity(int newCapacity) {
        if (newCapacity <= capacity) return;
        data = new float[newCapacity];
        capacity = newCapacity;
        position = 0;
    }

    public void clear() {
        position = 0;
    }

}
