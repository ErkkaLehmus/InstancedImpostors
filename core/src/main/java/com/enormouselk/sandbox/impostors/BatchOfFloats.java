package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.math.Vector3;

/**
 * A custom class to replace FloatBuffer
 * Surely this could be optimized ?
 */
public class BatchOfFloats {
    public float[] data;
    public int capacity;
    public int position;
    private final int id;
    private final FloatStreamer streamer;

    public BatchOfFloats(int id, int capacity, FloatStreamer streamer) {
        this.id = id;
        this.capacity = capacity;
        data = new float[capacity];
        this.streamer = streamer;
    }

    public void position(int newPos) {
        this.position = newPos;
    }

    public boolean isEmpty()
    {
        return position == 0;
    }

    public void put(float val) {

        if (position >= capacity) {
            if (streamer != null)
                streamer.flush(id);
        }

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

    public interface FloatStreamer {
        void flush(int id);
    }

}
