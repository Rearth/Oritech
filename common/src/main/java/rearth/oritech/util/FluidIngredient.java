package rearth.oritech.util;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.architectury.fluid.FluidStack;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

// Inspired by Immersive Engineering https://github.com/BluSunrize/ImmersiveEngineering/blob/1.21.1/src/api/java/blusunrize/immersiveengineering/api/crafting/FluidTagInput.java
public class FluidIngredient implements Predicate<FluidStack> {
    public static final MapCodec<FluidIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.mapEither(
            TagKey.codec(RegistryKeys.FLUID).fieldOf("tag"),
            Identifier.CODEC.fieldOf("fluid")
        ).forGetter(t -> t.fluidTag),
        Codec.LONG.fieldOf("amount").forGetter(t -> t.amount)
    ).apply(inst, FluidIngredient::new));

    protected final Either<TagKey<Fluid>, Identifier> fluidTag;
    protected final long amount;

    public static final FluidIngredient EMPTY = new FluidIngredient(Identifier.of("empty"), 0);

    public FluidIngredient(Either<TagKey<Fluid>, Identifier> fluidTag, long amount) {
        this.fluidTag = fluidTag;
        this.amount = amount;
    }

    public FluidIngredient(TagKey<Fluid> fluidTag, long amount) {
        this(Either.left(fluidTag), amount);
    }

    public FluidIngredient(Identifier fluid, long amount) {
        this(TagKey.of(RegistryKeys.FLUID, fluid), amount);
    }

    public FluidIngredient(Fluid fluid, long amount) {
        this(Registries.FLUID.getKey(fluid).get().getValue(), amount);
    }

    public FluidIngredient(FluidStack fluidStack) {
        this(fluidStack.getFluid(), fluidStack.getAmount());
    }

    @Override
    public boolean test(@Nullable FluidStack fluidStack) {
        return matchesFluid(fluidStack) && fluidStack.getAmount() >= this.amount;
    }

    public boolean matchesFluid(Fluid fluid) {
        return fluidTag.map(
            tag -> fluid.isIn(tag),
            id -> Registries.FLUID.getKey(fluid).isPresent() && id == Registries.FLUID.getKey(fluid).get().getValue());
    }

    public boolean matchesFluid(@Nullable FluidStack fluidStack) {
        if (fluidStack == null)
            return false;
        return matchesFluid(fluidStack.getFluid());
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public long getAmount() {
        return amount;
    }
    
}
