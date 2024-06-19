package rearth.oritech.util;


import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

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
        var variant = FluidVariant.of(Registries.FLUID.get(Identifier.of(nbt.getString("variant"))));
        return new FluidStack(variant, amount);
    }
    
    public NbtCompound toNbt(NbtCompound nbt) {
        nbt.putString("variant", Registries.FLUID.getId(variant.getFluid()).toString());
        nbt.putLong("amount", amount);
        return nbt;
    }
    
    public static void toNbt(NbtCompound nbt, FluidStack stack) {
        stack.toNbt(nbt);
    }
    
}
