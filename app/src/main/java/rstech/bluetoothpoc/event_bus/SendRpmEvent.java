package rstech.bluetoothpoc.event_bus;

/**
 * Created by raulstriglio on 9/16/17.
 */

public class SendRpmEvent {

    private String speed;
    private String rpm;

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getRpm() {
        return rpm;
    }

    public void setRpm(String rpm) {
        this.rpm = rpm;
    }
}
