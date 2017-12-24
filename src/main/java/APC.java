import javax.sound.midi.MidiDevice;

public class APC {
    private String deviceName;
    private MidiDevice midiDevice;


    public APC APC(String deviceName, MidiDevice midiDevice) {
        APC apc = new APC();
        apc.setDeviceName(deviceName);
        apc.setMidiDevice(midiDevice);
        return apc;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public MidiDevice getMidiDevice() {
        return midiDevice;
    }

    public void setMidiDevice(MidiDevice midiDevice) {
        this.midiDevice = midiDevice;
    }
}
