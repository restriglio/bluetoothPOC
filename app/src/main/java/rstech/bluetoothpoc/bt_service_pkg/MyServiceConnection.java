package rstech.bluetoothpoc.bt_service_pkg;

import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;

import java.io.IOException;

import rstech.bluetoothpoc.MainActivity;
import rstech.bluetoothpoc.obd_reader.AbstractGatewayService;
import rstech.bluetoothpoc.obd_reader.ObdCommandJob;
import rstech.bluetoothpoc.obd_reader.ObdConfig;
import rstech.bluetoothpoc.obd_reader.ObdGatewayService;

/**
 * Created by raulstriglio on 9/15/17.
 */

public class MyServiceConnection implements ServiceConnection {

    private AbstractGatewayService mService;
    private Context mContext;
    private boolean isServiceBound;
    private BluetoothSocket mSock = null;

    /*private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (mService != null && mService.isRunning() && mService.queueEmpty()) {
                queueCommands();
            }
            // run again in period defined in preferences
            new Handler().postDelayed(mQueueCommands, 4000);
        }
    };*/


    private void queueCommands() {
        if (isServiceBound) {
            for (ObdCommand Command : ObdConfig.getCommands()) {
                if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Command.getName(), true))
                    mService.queueJob(new ObdCommandJob(Command));
            }
        }
    }

    /*public Runnable getmQueueCommands(){
        return mQueueCommands;
    }*/

    public MyServiceConnection(AbstractGatewayService service, Context context, BluetoothSocket sock) {
        mService = service;
        mContext = context;
        mSock = sock;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {

        Log.d("SVTEST", "Activity service connected");
        ObdGatewayService.LocalBinder serviceBinder = (ObdGatewayService.LocalBinder) binder;
        mService = serviceBinder.getService();

        try {
            mService.setContext(mContext);
            mService.setmSock(mSock);
            queueCommands();
            mService.startService();
            isServiceBound = true;
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        isServiceBound = false;
    }
}
