package rstech.bluetoothpoc.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;

import rstech.bluetoothpoc.R;

/**
 * Created by raulstriglio on 8/12/17.
 */

public  class ButtonClicked {

    public ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
    public static BluetoothAdapter bluetoothAdapter = null;
    public ArrayAdapter<String> detectedAdapter;
    private Context context;

    public ButtonClicked(Context context){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        arrayListBluetoothDevices = new ArrayList<>();
        detectedAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_single_choice);
        this.context = context;
    }


    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                Toast.makeText(context, "ACTION_FOUND", Toast.LENGTH_SHORT).show();

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(arrayListBluetoothDevices.size()<1) // this checks if the size of bluetooth device is 0,then add the
                {                                           // device to the arraylist.
                    detectedAdapter.add(device.getName()+"\n"+device.getAddress());
                    arrayListBluetoothDevices.add(device);
                    detectedAdapter.notifyDataSetChanged();
                }
                else
                {
                    boolean flag = true;    // flag to indicate that particular device is already in the arlist or not
                    for(int i = 0; i<arrayListBluetoothDevices.size();i++)
                    {
                        if(device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress()))
                        {
                            flag = false;
                        }
                    }
                    if(flag == true)
                    {
                        detectedAdapter.add(device.getName()+"\n"+device.getAddress());
                        arrayListBluetoothDevices.add(device);
                        detectedAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    public void notifyChanges(){
        detectedAdapter.notifyDataSetChanged();
    }
}