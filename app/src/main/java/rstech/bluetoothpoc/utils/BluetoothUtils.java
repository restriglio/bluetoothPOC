package rstech.bluetoothpoc.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.UUID;

import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_ADDRESS;
import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_UUID;

/**
 * Created by raulstriglio on 8/26/17.
 */

public class BluetoothUtils {

    private static BluetoothDevice actual;
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothSocket socket;

    public static boolean connectAutomatically(Context context, BluetoothAdapter bluetoothAdapter) {

        String uuid = PreferenceManager.getDefaultSharedPreferences(context).getString(BLUETOOTH_UUID, "");
        String address = PreferenceManager.getDefaultSharedPreferences(context).getString(BLUETOOTH_ADDRESS, "");

        if (!address.isEmpty()) {

            if (uuid != null && !uuid.isEmpty()) {
                MY_UUID = UUID.fromString(uuid);
            }

            try {
                actual = bluetoothAdapter.getRemoteDevice(address);
                socket = actual.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();

                return true;

            } catch (IOException e) {
                e.printStackTrace();

                return false;
            }
        }

        return false;
    }

    public static BluetoothSocket getSocket(){
        return socket;
    }

    public static BluetoothDevice getActualDevice() {
        return actual;
    }

}
