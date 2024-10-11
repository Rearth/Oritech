package rearth.oritech.util;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import java.util.stream.IntStream;

public class SimpleSidedInventory extends SimpleInventory implements SidedInventory {

    public final InventorySlotAssignment slotAssignment;

    public SimpleSidedInventory(int size, InventorySlotAssignment slotAssignment) {
        super(size);
        this.slotAssignment = slotAssignment;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slotAssignment.isOutput(slot);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return slotAssignment.isInput(slot);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        switch (side) {
            case Direction.UP:
                return IntStream.range(slotAssignment.inputStart(), slotAssignment.inputStart() + slotAssignment.inputCount()).toArray();
            case Direction.DOWN:
                return IntStream.range(slotAssignment.outputStart(), slotAssignment.outputStart() + slotAssignment.outputCount()).toArray();
            default:
                return IntStream.concat(
                    IntStream.range(slotAssignment.inputStart(), slotAssignment.inputStart() + slotAssignment.inputCount()),
                    IntStream.range(slotAssignment.outputStart(), slotAssignment.outputStart() + slotAssignment.outputCount())).toArray();
        }
    }
}
