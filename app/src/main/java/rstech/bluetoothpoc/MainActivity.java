package rstech.bluetoothpoc;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.pires.obd.commands.ObdCommand;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;
import rstech.bluetoothpoc.bluetooth.ButtonClicked;
import rstech.bluetoothpoc.bt_devices_list.BluetoothDeviceObject;
import rstech.bluetoothpoc.bt_devices_list.PairedDevicesAdapter;
import rstech.bluetoothpoc.bt_service_pkg.MyServiceConnection;
import rstech.bluetoothpoc.event_bus.BluetoothConnectedStatusEvent;
import rstech.bluetoothpoc.event_bus.BusProvider;
import rstech.bluetoothpoc.event_bus.SendRpmEvent;
import rstech.bluetoothpoc.event_bus.StateUpdateCommandEvent;
import rstech.bluetoothpoc.obd_reader.AbstractGatewayService;
import rstech.bluetoothpoc.obd_reader.ObdCommandJob;
import rstech.bluetoothpoc.obd_reader.ObdConfig;
import rstech.bluetoothpoc.obd_reader.ObdGatewayService;
import rstech.bluetoothpoc.utils.BluetoothUtils;

import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_ADDRESS;
import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_UUID;
import static rstech.bluetoothpoc.utils.OdbUtils.LookUpCommand;
import static rstech.bluetoothpoc.utils.OdbUtils.updateTripStatistic;


@RuntimePermissions
public class MainActivity extends AppCompatActivity implements PairedDevicesAdapter.PairedDevicesCallback {

    private static final String TAG = "MainActivity";
    private static final int REQ_CODE = 676;
    List<BluetoothDeviceObject> listParedDevices;

    private RecyclerView mRvPairedDevices;
    private PairedDevicesAdapter pairedDevicesAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private RelativeLayout empty_state;

    private ButtonClicked clicked;
    private RelativeLayout buttonSettings;
    BluetoothDevice bdDevice;

    boolean isServiceBound = false;
    boolean serviceStarted = false;
    private boolean preRequisites = true;

    private TextView btStatusTextView;
    private TextView obdStatusTextView;
    private TextView obdStatusTextView1;
    private TextView tv_select_another_device;
    private SharedPreferences prefs;
    private AbstractGatewayService mService;
    private MyServiceConnection serviceConn;

    private void doUnbindService() {
        if (isServiceBound) {
            if (mService.isRunning()) {
                mService.stopService();
                if (preRequisites)
                    btStatusTextView.setText("Bluetooth OK");
            }
            unbindService(serviceConn);
            isServiceBound = false;
            obdStatusTextView.setText("ODB Disconected");
        }
    }

    @Override
    @NeedsPermission({Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_PRIVILEGED})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        btStatusTextView = (TextView) findViewById(R.id.btStatusTextView);
        obdStatusTextView = (TextView) findViewById(R.id.obdStatusTextView);
        obdStatusTextView1 = (TextView) findViewById(R.id.obdStatusTextView1);
        tv_select_another_device = (TextView) findViewById(R.id.tv_select_another_device);

        buttonSettings = (RelativeLayout) findViewById(R.id.buttonSettings);
        empty_state = (RelativeLayout) findViewById(R.id.empty_state);
        listParedDevices = new ArrayList<>();
        clicked = new ButtonClicked(this);
        mRvPairedDevices = (RecyclerView) findViewById(R.id.rv_paired_devices);
        clicked.notifyChanges();

        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivityForResult(settingsIntent, REQ_CODE);
            }
        });

        tv_select_another_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                listParedDevices.clear();
                pairedDevicesAdapter.setList(new ArrayList<BluetoothDeviceObject>());


                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().
                        putString(BLUETOOTH_ADDRESS, "").apply();

                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().
                        putString(BLUETOOTH_UUID, "").apply();

                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivityForResult(settingsIntent, REQ_CODE);
            }
        });

        serviceConn = new MyServiceConnection(mService, MainActivity.this, BluetoothUtils.getSocket());
        listParedDevices = new ArrayList<>();
        pairedDevicesAdapter = new PairedDevicesAdapter(this, listParedDevices, this);


        mRvPairedDevices.setAdapter(pairedDevicesAdapter);
        layoutManager = new LinearLayoutManager(this);
        mRvPairedDevices.setLayoutManager(layoutManager);
        pairedDevicesAdapter.notifyDataSetChanged();

        BusProvider.getInstance().register(this);

        String uuid = PreferenceManager.getDefaultSharedPreferences(this).getString(BLUETOOTH_UUID, "");
        String address = PreferenceManager.getDefaultSharedPreferences(this).getString(BLUETOOTH_ADDRESS, "");

        getPairedDevices();

        if (!(uuid.isEmpty() && address.isEmpty())) {
            doBindService();
        }
    }

    private void doBindService() {
        /*if (!isServiceBound) {
            Log.d(TAG, "Binding OBD service..");
            if (preRequisites) {
                btStatusTextView.setText("Conectado");
                Intent serviceIntent = new Intent(this, ObdGatewayService.class);
                bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
            }
        }*/

        Intent intent = new Intent(this, ObdGatewayService.class);
        getApplicationContext().startService(intent);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //TODO do the magick
        getPairedDevices();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevice = clicked.bluetoothAdapter.getBondedDevices();

        String uuid = PreferenceManager.getDefaultSharedPreferences(this).getString(BLUETOOTH_UUID, "");
        String address = PreferenceManager.getDefaultSharedPreferences(this).getString(BLUETOOTH_ADDRESS, "");

        if (pairedDevice.size() > 0) {
            for (BluetoothDevice device : pairedDevice) {
                BluetoothDeviceObject bluetoothDeviceObject = new BluetoothDeviceObject();
                bluetoothDeviceObject.setAddress(device.getAddress());
                bluetoothDeviceObject.setName(device.getName());
                bluetoothDeviceObject.setBluetoothDevice(device);

                boolean connected = device.getUuids()[0].toString().equals(uuid) && device.getAddress().equals(address);
                if (connected) {
                    btStatusTextView.setText("Conectado");
                } else {
                    btStatusTextView.setText("No Conectado");
                }
                bluetoothDeviceObject.setConnected(connected);
                listParedDevices.add(bluetoothDeviceObject);
            }
            pairedDevicesAdapter.setList(listParedDevices);
            pairedDevicesAdapter.notifyDataSetChanged();
            empty_state.setVisibility(View.GONE);
            mRvPairedDevices.setVisibility(View.VISIBLE);

        } else {

            /*PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().
                    putString(BLUETOOTH_ADDRESS, "").apply();

            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().
                    putString(BLUETOOTH_UUID, "").apply();*/

            empty_state.setVisibility(View.VISIBLE);
            mRvPairedDevices.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeviceClick(BluetoothDeviceObject bluetoothDeviceObject, int position) {
        bdDevice = bluetoothDeviceObject.getBluetoothDevice();
        try {

            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().
                    putString(BLUETOOTH_ADDRESS, bdDevice.getAddress()).apply();

            if (bdDevice.fetchUuidsWithSdp()) {
                ParcelUuid[] uuids = bdDevice.getUuids();
                if (uuids != null && uuids.length > 0) {
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().
                            putString(BLUETOOTH_UUID, uuids[0].toString()).apply();
                }
            }

            if (!serviceStarted) {
                doBindService();
            }

            pairedDevicesAdapter.updateElement(true, position);
            pairedDevicesAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Subscribe
    public void stateUpdate(ObdCommandJob job) {

       // final ObdCommandJob job = stateUpdateCommandEvent.getJob2();

        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        final String cmdID = LookUpCommand(cmdName);

        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
            if (cmdResult != null) {
                //TODO Log this: cmdResult.toLowerCase();
            }
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE)) {
            //TODO stopLiveData();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = "Not supported";
        } else {
            cmdResult = job.getCommand().getFormattedResult();
            //TODO obdStatusTextView.setText(getString(R.string.status_obd_data));
        }

        // TODO use hashmap commandResult.put(cmdID, cmdResult);
        updateTripStatistic(job, cmdID);
    }

    @Subscribe
    public void updateView(SendRpmEvent sendRpmEvent) {
        obdStatusTextView.setText(sendRpmEvent.getSpeed());
        obdStatusTextView1.setText(sendRpmEvent.getRpm());
    }

    @Subscribe
    public void updateConnected(BluetoothConnectedStatusEvent bluetoothConnectedStatusEvent) {
        if (bluetoothConnectedStatusEvent.isConnected()) {
            btStatusTextView.setText("Conectado");
        } else {
            btStatusTextView.setText("No Conectado");
        }
    }
}

