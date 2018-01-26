package rstech.bluetoothpoc.bluetooth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_UUID;

public class BluetoothManager {

    private static final String TAG = BluetoothManager.class.getName();
    //private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public static BluetoothSocket connect(BluetoothDevice dev, Context context) throws IOException {
        BluetoothSocket sock = null;
        BluetoothSocket sockFallback = null;

        Log.d(TAG, "Starting Bluetooth connection..");
        try {

            final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            String uuid = PreferenceManager.getDefaultSharedPreferences(context).getString(BLUETOOTH_UUID, "");

            if (!(uuid == null || uuid.isEmpty())) {
                sock = dev.createRfcommSocketToServiceRecord(UUID.fromString(uuid));
            } else {
                sock = dev.createRfcommSocketToServiceRecord(SERIAL_UUID);
            }

            sock.connect();

        } catch (Exception e1) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            Class<?> clazz = sock.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                Object[] params = new Object[] {Integer.valueOf(1)};
                sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                sockFallback.connect();
                sock = sockFallback;
            } catch (Exception e2) {
                Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                throw new IOException(e2.getMessage());
            }
        }
        return sock;
    }
}