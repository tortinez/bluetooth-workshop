package edu.upc.mcia.androidpracticabluetooth.command;

public class BitsCommand {

    public boolean bit0;
    public boolean bit1;
    public boolean bit2;
    public boolean bit3;

    public int encode() {
        int value = 0;
        value += this.bit0 ? 1 : 0;
        value += this.bit1 ? 2 : 0;
        value += this.bit2 ? 4 : 0;
        value += this.bit3 ? 8 : 0;
        return value;
    }

    public static BitsCommand decode(int value) {
        BitsCommand command = new BitsCommand();
        command.bit0 = (value & 0x01) != 0;
        command.bit1 = (value & 0x02) != 0;
        command.bit2 = (value & 0x04) != 0;
        command.bit3 = (value & 0x08) != 0;
        return command;
    }

    @Override
    public String toString() {
        return "[bit0=" + (bit0 ? "1" : "0")
                + ", bit1=" + (bit1 ? "1" : "0")
                + ", bit2=" + (bit2 ? "1" : "0")
                + ", bit3=" + (bit3 ? "1" : "0")
                + "]";
    }
}
