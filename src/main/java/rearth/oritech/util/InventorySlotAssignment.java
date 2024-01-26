package rearth.oritech.util;

public record InventorySlotAssignment(int inputStart, int inputCount, int outputStart, int outputCount) {
    public int inputToRealSlot(int input) {
        return input + inputStart;
    }

    public int outputToRealSlot(int output) {
        return output + outputStart;
    }
}
