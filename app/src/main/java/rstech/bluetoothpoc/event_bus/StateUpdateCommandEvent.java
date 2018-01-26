package rstech.bluetoothpoc.event_bus;

import rstech.bluetoothpoc.obd_reader.ObdCommandJob;

/**
 * Created by raulstriglio on 9/16/17.
 */

public class StateUpdateCommandEvent {

    private ObdCommandJob job2;


    public ObdCommandJob getJob2() {
        return job2;
    }

    public void setJob2(ObdCommandJob job2) {
        this.job2 = job2;
    }
}
