package rearth.oritech.util;


import com.mojang.serialization.Codec;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public record FluidStack(FluidVariant variant, long amount) {
    
    public static final Endec<FluidStack> ENDEC = StructEndecBuilder.of(
      CodecUtils.toEndec(FluidVariant.CODEC).fieldOf("variant", FluidStack::variant),
      Endec.LONG.fieldOf("amount", FluidStack::amount),
      FluidStack::new
    );
    
    public static final Codec<FluidStack> CODEC = CodecUtils.toCodec(ENDEC);
    public static final PacketCodec<RegistryByteBuf, FluidStack> PACKET_CODEC = PacketCodec.tuple(FluidVariant.PACKET_CODEC, FluidStack::variant, PacketCodecs.VAR_LONG, FluidStack::amount, FluidStack::new);
    
    public FluidStack(Fluid variant, long amount) {
        this(FluidVariant.of(variant), amount);
    }
    
    @Override
    public String toString() {
        return "FluidStack{" + "variant=" + variant + ", amount=" + amount + '}';
    }
    
    public static FluidStack fromNbt(NbtCompound nbt) {
        var amount = nbt.getLong("amount");
        var nbtVariant = nbt.get("variant");
        // FluidVariant writes to nbt as variant:{fluid:"fluid_id"}
        // but but this reads/writes as variant:"fluid_id"
        // Support either format while reading here
        var variantID = (nbtVariant.getType() == NbtElement.STRING_TYPE)
            ? Identifier.of(nbtVariant.asString())
            : Identifier.of(((NbtCompound)nbtVariant).getString("fluid"));
        var variant = FluidVariant.of(Registries.FLUID.get(variantID));
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
