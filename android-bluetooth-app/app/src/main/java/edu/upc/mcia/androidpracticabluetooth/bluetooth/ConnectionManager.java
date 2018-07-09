package edu.upc.mcia.androidpracticabluetooth.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import edu.upc.mcia.androidpracticabluetooth.command.BitsCommand;
import edu.upc.mcia.androidpracticabluetooth.command.BytesCommand;

public class ConnectionManager {

    // Constants
    private static final String TAG = "BT_MANAGER";
    private final static UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int ACTION_SEARCHING_DEVICE = 1;
    public static final int ACTION_SEARCHING_FAILED = 2;
    public static final int ACTION_CONNECTING = 3;
    public static final int ACTION_CONNECTED = 4;
    public static final int ACTION_DISCONNECTED = 5;
    public static final int ACTION_BITS_RECEPTION = 6;
    public static final int ACTION_BYTES_RECEPTION = 7;

    // Reception modes
    public static final int MODE_BITS = 0;
    public static final int MODE_BYTES = 1;

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;

    // Threads
    private ConnectThread connectThread;
    private CommunicationThread communicationThread;

    // Handlers & Events
    private final BluetoothEventHandler handler;

    // Internal variables
    private final AtomicBoolean forceDisconnect;
    private final AtomicInteger receptionMode;
    private final AtomicInteger receptionLength;

    public ConnectionManager(BluetoothAdapter bluetoothAdapter, BluetoothEventHandler.BluetoothEventListener listener) {
        this.forceDisconnect = new AtomicBoolean(false);
        this.receptionMode = new AtomicInteger(MODE_BITS);
        this.receptionLength = new AtomicInteger(1);
        this.bluetoothAdapter = bluetoothAdapter;
        this.handler = new BluetoothEventHandler(listener);
    }

    public synchronized void turnOn() {
        handler.obtainMessage(ACTION_SEARCHING_DEVICE).sendToTarget();
        turnOff();
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().startsWith("RN")) {
                Log.d(TAG, "S'ha trobat el RN-42");
                connectThread = new ConnectThread(device);
                connectThread.start();
                return;
            }
        }
        Log.e(TAG, "NO ES TROBA EL RN-42!");
        handler.obtainMessage(ACTION_SEARCHING_FAILED).sendToTarget();
    }

    public synchronized void turnOff() {
        forceDisconnect.set(true);
        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    public void setReceptionMode(int newMode) {
        if (newMode == MODE_BITS || newMode == MODE_BYTES) {
            receptionMode.set(newMode);
        } else {
            throw new IllegalArgumentException("Invalid bluetooth reception mode: " + newMode);
        }
    }

    public void setReceptionLength(int newLength) {
        if (newLength > 0 && newLength < BytesCommand.MAX_LENGTH) {
            receptionLength.set(newLength);
            if (communicationThread != null) {
                communicationThread.onReceptionLengthChange(newLength);
            }
        } else {
            throw new IllegalArgumentException("Invalid bluetooth reception length: " + newLength);
        }
    }

    public synchronized void sendCommand(BitsCommand command) throws Exception {
        communicationThread.write(command);
    }

    public synchronized void sendCommand(BytesCommand command) throws Exception {
        communicationThread.write(command);
    }

    /**
     * This thread obtains a connection to the Bluetooth module
     */
    private class ConnectThread extends Thread {

        public static final String TAG = "CONNECT";
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice bluetoothDevice) {
            forceDisconnect.set(false);
            BluetoothSocket temp = null;
            device = bluetoothDevice;
            try {
                temp = device.createRfcommSocketToServiceRecord(UUID_SPP);
            } catch (IOException ioe) {
                Log.e(TAG, "Error en crear socket: " + ioe.toString());
            }
            socket = temp;
        }

        public void run() {
            int retryCount = 1;
            Boolean connexioEstablerta = false;
            if (socket == null) {
                Log.e(TAG, "Error: socket is null!");
                return;
            }
            Log.d(TAG, "-- Connect Thread started --");
            bluetoothAdapter.cancelDiscovery();
            handler.obtainMessage(ACTION_CONNECTING, retryCount, 0).sendToTarget();
            while (!connexioEstablerta && !forceDisconnect.get()) {
                try {
                    socket.connect();
                    connexioEstablerta = true;
                } catch (IOException ioe) {
                    connexioEstablerta = false;
                    retryCount++;
                    handler.obtainMessage(ACTION_CONNECTING, retryCount, 0).sendToTarget();
                    Log.e(TAG, "Error connectant: " + ioe.getMessage());
                    try {
                        Thread.sleep(500); // Espera abans de tornar a intentar
                    } catch (InterruptedException ie) {
                    }
                }
            }
            if (connexioEstablerta) {
                Log.w(TAG, "S'ha establert la connexio!");
                communicationThread = new CommunicationThread(socket);
                communicationThread.start();
            }
            Log.d(TAG, "-- Connect Thread closed --");
        }

        public void cancel() {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * This thread handles the Bluetooth Communication
     */
    private class CommunicationThread extends Thread {
        public static final String TAG = "COMMUNICATE";
        private final BluetoothSocket socket;
        private final InputStream input;
        private final OutputStream output;

        private BytesCommand bytesCommand;

        public CommunicationThread(BluetoothSocket bluetoothSocket) {
            socket = bluetoothSocket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            input = tmpIn;
            output = tmpOut;
        }

        public void run() {
            Log.d(TAG, "-- Communication Thread started --");
            handler.obtainMessage(ACTION_CONNECTED).sendToTarget();
            bytesCommand = new BytesCommand(receptionLength.get());
            int value;
            try {
                while (!forceDisconnect.get()) {
                    value = input.read();
                    switch (receptionMode.get()) {
                        case MODE_BITS:
                            handler.obtainMessage(ACTION_BITS_RECEPTION, BitsCommand.decode(value)).sendToTarget();
                            break;
                        case MODE_BYTES:
                            if (bytesCommand.addValue(value)) {
                                handler.obtainMessage(ACTION_BYTES_RECEPTION, bytesCommand).sendToTarget();
                                bytesCommand = new BytesCommand(receptionLength.get());
                            }
                            break;
                    }
                }
            } catch (Exception e) {
            }
            if (!forceDisconnect.get()) {
                Log.w(TAG, "S'ha perdut la connexio bluetooth!");
                handler.obtainMessage(ACTION_DISCONNECTED).sendToTarget();
            }
            Log.d(TAG, "-- Communication Thread closed --");
        }

        public void write(BitsCommand command) throws Exception {
            output.write(0x0F & command.encode());
        }

        public void write(BytesCommand command) throws Exception {
            output.write(command.toByteArray());
        }

        public void onReceptionLengthChange(int newLength) {
            this.bytesCommand = new BytesCommand(receptionLength.get());
        }

        public void cancel() {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }
}
