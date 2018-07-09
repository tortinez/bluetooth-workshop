package edu.upc.mcia.androidpracticabluetooth.command;

import java.util.Arrays;

public class BytesCommand {

    public static final int START_BYTE = 0x55;
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 16;

    public final int[] array;
    private int count;

    public BytesCommand(int length) {
        this.array = new int[(length > MAX_LENGTH) ? MAX_LENGTH : length];
        this.count = 0;
    }

    public BytesCommand(int[] initialValues) {
        this.array = initialValues;
        this.count = initialValues.length;
    }

    public boolean addValue(int value) {
        if (count == 0) {
            if (value == START_BYTE) {
                array[0] = value;
                count++;
            }
        } else if (!isComplete()) {
            array[count] = value;
            count++;
        }
        return isComplete();
    }

    public boolean isComplete() {
        return count >= array.length;
    }

    @Override
    public String toString() {
        return "[array=" + Arrays.toString(array) + "]";
    }

    public String[] toStringArray() {
        String[] strs = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            strs[i] = Integer.toString(array[i]);
        }
        return strs;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = (byte) (array[i] & 0xFF);
        }
        return bytes;
    }

}
