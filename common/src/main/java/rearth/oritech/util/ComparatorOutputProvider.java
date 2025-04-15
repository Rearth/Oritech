package rearth.oritech.util;

import net.minecraft.item.ItemStack;
import rearth.oritech.util.fluid.FluidApi;

public interface ComparatorOutputProvider {

	static int getItemStackComparatorOutput(ItemStack stack) {
		return (int) ((stack.getCount() / (float) stack.getMaxCount()) * 15);
	}

	static int getFluidStorageComparatorOutput(FluidApi.SingleSlotStorage storage) {
		return (int) ((storage.getStack().getAmount() / (float) storage.getCapacity()) * 15);
	}

	int getComparatorOutput();
}
