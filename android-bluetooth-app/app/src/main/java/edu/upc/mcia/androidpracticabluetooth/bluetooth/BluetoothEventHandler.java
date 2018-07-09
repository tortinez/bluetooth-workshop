package edu.upc.mcia.androidpracticabluetooth.bluetooth;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class BluetoothEventHandler extends Handler {

    private final WeakReference<BluetoothEventListener> listenerWeakRef;

    public BluetoothEventHandler(BluetoothEventListener listener) {
        this.listenerWeakRef = new WeakReference<>(listener);
    }

    @Override
    public void handleMessage(Message msg) {
        BluetoothEventListener listener = listenerWeakRef.get();
        if (listener != null)
            listener.handleBluetoothEvent(msg);
    }

    public interface BluetoothEventListener {
        public void handleBluetoothEvent(Message msg);
    }

}
