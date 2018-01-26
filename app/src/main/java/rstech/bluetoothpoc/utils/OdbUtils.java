package rstech.bluetoothpoc.utils;

import android.util.Log;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.RuntimeCommand;
import com.github.pires.obd.enums.AvailableCommandNames;

import rstech.bluetoothpoc.event_bus.BusProvider;
import rstech.bluetoothpoc.event_bus.SendRpmEvent;
import rstech.bluetoothpoc.obd_reader.ObdCommandJob;

/**
 * Created by raulstriglio on 8/20/17.
 */

public class OdbUtils {

    public static String LookUpCommand(String txt) {
        for (AvailableCommandNames item : AvailableCommandNames.values()) {
            if (item.getValue().equals(txt))
                return item.name();
        }
        return txt;
    }


    public static void updateTripStatistic(final ObdCommandJob job, final String cmdID) {

        try {


            if (cmdID.equals(AvailableCommandNames.SPEED.toString())) {
                SpeedCommand command = (SpeedCommand) job.getCommand();
                command.getMetricSpeed(); //TODO use this method to get spedd

                SendRpmEvent sendRpmEvent = new SendRpmEvent();
                sendRpmEvent.setSpeed(String.valueOf(command.getMetricSpeed()));
                BusProvider.getInstance().post(sendRpmEvent);

            } else if (cmdID.equals(AvailableCommandNames.ENGINE_RPM.toString())) {
                RPMCommand command = (RPMCommand) job.getCommand();
                command.getRPM(); //TODO use this method to get RPM

                SendRpmEvent sendRpmEvent = new SendRpmEvent();
                sendRpmEvent.setRpm(String.valueOf(command.getRPM()));
                BusProvider.getInstance().post(sendRpmEvent);

            } else if (cmdID.endsWith(AvailableCommandNames.ENGINE_RUNTIME.toString())) {
                RuntimeCommand command = (RuntimeCommand) job.getCommand();
                command.getFormattedResult(); //TODO use this method to get engine runtime (?)

                SendRpmEvent sendRpmEvent = new SendRpmEvent();
                sendRpmEvent.setRpm(String.valueOf(command.getFormattedResult()));
                BusProvider.getInstance().post(sendRpmEvent);

            }
        } catch (Exception e) {
            Log.d("OdbUtils", "mensaje: " + e.getMessage());

            SendRpmEvent sendRpmEvent = new SendRpmEvent();
            sendRpmEvent.setRpm("Error");
            BusProvider.getInstance().post(sendRpmEvent);

        }
    }

}
