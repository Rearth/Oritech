package rearth.oritech.util;


import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;

public record FluidStack(FluidVariant variant, long amount) {
    
    public FluidStack(Fluid variant, long amount) {
        this(FluidVariant.of(variant), amount);
    }
    
    @Override
    public String toString() {
        return "FluidStack{" + "variant=" + variant + ", amount=" + amount + '}';
    }
    
    public static FluidStack fromNbt(NbtCompound nbt) {
        var amount = nbt.getLong("amount");
        var variant = FluidVariant.fromNbt(nbt.getCompound("variant"));
        return new FluidStack(variant, amount);
    }
    
    public NbtCompound toNbt(NbtCompound nbt) {
        nbt.put("variant", variant.toNbt());
        nbt.putLong("amount", amount);
        return nbt;
    }
    
    public static void toNbt(NbtCompound nbt, FluidStack stack) {
        stack.toNbt(nbt);
    }
    
}
