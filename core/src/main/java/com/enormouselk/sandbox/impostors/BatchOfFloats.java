package com.enormouselk.sandbox.impostors;

import com.badlogic.gdx.math.Vector3;

import java.util.Arrays;

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
                streamer.flush(id,true);

            clear();
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

        int remaining = capacity-position;
        if (remaining == 0)
        {
            if (streamer != null)
                streamer.flush(id,true);
            clear();
            remaining = capacity;
        }

        int overFlow = values.length - (remaining);

        if (overFlow > 0) {
            //int fromIndex = values.length-overFlow;
            int fromIndex = 0;
            int toBeCopied = overFlow;
            if (toBeCopied > remaining) toBeCopied = remaining;

            while (toBeCopied > 0)
            {
                System.arraycopy(values,fromIndex,data,position,toBeCopied);
                if (streamer != null)
                    streamer.flush(id,true);
                clear();
                fromIndex+=toBeCopied;

                toBeCopied = values.length-fromIndex;

                if (toBeCopied < capacity) break;
                toBeCopied = capacity;

            }

            clear();
            if (toBeCopied > 0) {
                //data = Arrays.copyOfRange(values, fromIndex, fromIndex+capacity);
                System.arraycopy(values,fromIndex,data,position,toBeCopied);
                //data = Arrays.copyOfRange(values, fromIndex, toBeCopied);
                position = toBeCopied;
            }

        }
        else
        {
            System.arraycopy(values,0,data,position,values.length);
            position += values.length;
        }
    }

    public float[] getData()
    {
        return Arrays.copyOfRange(data,0,position);
    }


    public void clear() {
        position = 0;
    }

    public interface FloatStreamer {
        void flush(int id, boolean flushBatch);
    }

}
