package rearth.oritech.util;

public record InventorySlotAssignment(int inputStart, int inputCount, int outputStart, int outputCount) {
    public int inputToRealSlot(int input) {
        return input + inputStart;
    }

    public boolean isInput(int slot) {
        return slot >= inputStart && slot < inputStart + inputCount;
    }

    public int outputToRealSlot(int output) {
        return output + outputStart;
    }

    public boolean isOutput(int slot) {
        return slot >= outputStart && slot < outputStart + outputCount;
    }
}
