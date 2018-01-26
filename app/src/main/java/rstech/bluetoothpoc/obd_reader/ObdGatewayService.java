package rstech.bluetoothpoc.obd_reader;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnsupportedCommandException;

import rstech.bluetoothpoc.MainActivity;
import rstech.bluetoothpoc.bluetooth.ButtonClicked;
import rstech.bluetoothpoc.event_bus.BluetoothConnectedStatusEvent;
import rstech.bluetoothpoc.event_bus.BusProvider;
import rstech.bluetoothpoc.event_bus.StateUpdateCommandEvent;
import rstech.bluetoothpoc.obd_reader.ObdCommandJob.*;
import rstech.bluetoothpoc.utils.BluetoothUtils;
import rstech.bluetoothpoc.utils.Constants;
import java.io.IOException;

import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_ADDRESS;
import static rstech.bluetoothpoc.utils.Constants.BLUETOOTH_UUID;

/**
 * Created by raulstriglio on 8/20/17.
 */

public class ObdGatewayService extends AbstractGatewayService {

    private static final String TAG = ObdGatewayService.class.getName();
    SharedPreferences prefs;

    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("SVTEST", "Loc service ONBIND");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("SVTEST", "Loc service ONUNBIND");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            startService();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void setmSock(BluetoothSocket sock){
        this.sock  = sock;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void startService() throws IOException {
        prefs = PreferenceManager.getDefaultSharedPreferences(ObdGatewayService.this);
        String uuid = PreferenceManager.getDefaultSharedPreferences(this).getString(BLUETOOTH_UUID, "");
        String address = PreferenceManager.getDefaultSharedPreferences(this).getString(BLUETOOTH_ADDRESS, "");

        BluetoothConnectedStatusEvent bluetoothConnectedStatusEvent = new BluetoothConnectedStatusEvent();
        bluetoothConnectedStatusEvent.setConnected(BluetoothUtils.connectAutomatically(this, ButtonClicked.bluetoothAdapter));
        BusProvider.getInstance().post(bluetoothConnectedStatusEvent);

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        dev = btAdapter.getRemoteDevice(address);

        for (ObdCommand Command : ObdConfig.getCommands()) {
            queueJob(new ObdCommandJob(Command));
        }

        try {
            startObdConnection();
        } catch (Exception e) {
            stopService();
            throw new IOException();
        }
    }

    /**
     * Start and configure the connection to the OBD interface.
     * <p/>
     * See http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
     *
     * @throws IOException
     */
    private void startObdConnection() throws IOException {

        isRunning = true;
        queueJob(new ObdCommandJob(new ObdResetCommand()));
        //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queueJob(new ObdCommandJob(new EchoOffCommand()));

    /*
     * Will send second-time based on tests.
     *
     * TODO this can be done w/o having to queue jobs by just issuing
     * command.run(), command.getResult() and validate the result.
     */
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(62)));

        // Get protocol from preferences
        final String protocol = prefs.getString(Constants.PROTOCOLS_LIST_KEY, "AUTO");
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));

        // Job for returning dummy data
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

        queueCounter = 0L;
    }

    /**
     * This method will add a job to the queue while setting its ID to the
     * internal queue counter.
     *
     * @param job the job to queue.
     */
    @Override
    public void queueJob(ObdCommandJob job) {
        // This is a good place to enforce the imperial units option
        job.getCommand().useImperialUnits(prefs.getBoolean(Constants.IMPERIAL_UNITS_KEY, false));

        // Now we can pass it along
        super.queueJob(job);
    }

    /**
     * Runs the queue until the service is stopped
     */
    protected void executeQueue() throws InterruptedException {


        while (!Thread.currentThread().isInterrupted()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();
                if (job.getState().equals(ObdCommandJobState.NEW)) {
                    Log.d(TAG, "Job state is NEW. Run it..");
                    job.setState(ObdCommandJobState.RUNNING);
                    if (BluetoothUtils.getSocket().isConnected()) {
                        job.getCommand().run(BluetoothUtils.getSocket().getInputStream(), BluetoothUtils.getSocket().getOutputStream());
                    } else {
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                        Log.e(TAG, "Can't run command on a closed socket.");
                    }
                } else
                    // log not new job
                    Log.e(TAG,
                            "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJobState.NOT_SUPPORTED);
                }
                Log.d(TAG, "Command not supported. -> " + u.getMessage());
            } catch (IOException io) {
                if (job != null) {
                    if (io.getMessage().contains("Broken pipe"))
                        job.setState(ObdCommandJobState.BROKEN_PIPE);
                    else
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "IO error. -> " + io.getMessage());
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "Failed to run command. -> " + e.getMessage());
            }

            if (job != null) {

                StateUpdateCommandEvent stateUpdateCommandEvent = new StateUpdateCommandEvent();
                stateUpdateCommandEvent.setJob2(job);
                //BusProvider.getInstance().post(stateUpdateCommandEvent);
                final ObdCommandJob job2 = job;
                /*((MainActivity) ctx).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) ctx).stateUpdate(job2);
                    }
                });*/



            }
        }
    }


    /**
     * Stop OBD connection and queue processing.
     */
    public void stopService() {

        jobsQueue.clear();
        isRunning = false;
        if (sock != null)
            try {
                sock.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        stopSelf();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public class LocalBinder extends Binder {
        public ObdGatewayService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ObdGatewayService.this;
        }
    }

}
