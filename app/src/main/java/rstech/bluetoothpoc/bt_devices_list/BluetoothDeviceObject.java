package rstech.bluetoothpoc.bt_devices_list;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * Created by raulstriglio on 9/16/17.
 */

public class BluetoothDeviceObject implements Serializable {

    private BluetoothDevice bluetoothDevice;
    private String name;
    private String address;
    private boolean connected;

    public BluetoothDeviceObject(){

    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
