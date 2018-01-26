package rstech.bluetoothpoc.event_bus;


import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;


/**
 * Created by raulstriglio on 9/16/17.
 */
public final class BusProvider {
    private static final Bus BUS = new Bus(ThreadEnforcer.ANY);

    public static Bus getInstance() {
        return BUS;
    }

    private BusProvider() {
        // No instances.
    }
}