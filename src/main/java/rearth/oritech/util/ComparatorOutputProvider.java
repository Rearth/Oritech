package rearth.oritech.util;

import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.fluid.FluidApi;

public interface ComparatorOutputProvider {

	static int getItemStackComparatorOutput(ItemStack stack) {
		return (int) ((stack.getCount() / (float) stack.getMaxStackSize()) * 15);
	}

	static int getFluidStorageComparatorOutput(FluidApi.SingleSlotStorage storage) {
		return (int) ((storage.getStack().getAmount() / (float) storage.getCapacity()) * 15);
	}

	int getComparatorOutput();
}
