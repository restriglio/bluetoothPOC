package rstech.bluetoothpoc.event_bus;

/**
 * Created by raulstriglio on 9/16/17.
 */

public class BluetoothConnectedStatusEvent {

    private boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
