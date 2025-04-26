package rearth.oritech.util;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.architectury.fluid.FluidStack;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

// Inspired by Immersive Engineering https://github.com/BluSunrize/ImmersiveEngineering/blob/1.21.1/src/api/java/blusunrize/immersiveengineering/api/crafting/FluidTagInput.java
public record FluidIngredient(Either<TagKey<Fluid>, Identifier> fluidContent, long amount) implements Predicate<FluidStack> {
    // public static final MapCodec<FluidIngredient> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    //     Codec.mapEither(
    //         TagKey.codec(RegistryKeys.FLUID).fieldOf("tag"),
    //         Identifier.CODEC.fieldOf("fluid")
    //     ).forGetter(t -> t.fluidContent),
    //     Codec.LONG.fieldOf("amount").forGetter(t -> t.amount)
    // ).apply(inst, FluidIngredient::new));

    public static Endec<TagKey<Fluid>> TAG_PREFIXED = 
                ;

    public static final Endec<FluidIngredient> FLUID_INGREDIENT_ENDEC = StructEndecBuilder.of(
        CodecUtils.eitherEndec(
            Endec.STRING.xmap(
                s -> {
                    if (s.charAt(0) != '#') throw new IllegalStateException("tag must start with #");
                    return TagKey.of(RegistryKeys.FLUID, Identifier.of(s.substring(1)));
                },
                tag -> "#" + tag.id()
            ),
            MinecraftEndecs.IDENTIFIER).fieldOf("fluid", FluidIngredient::fluidContent),
        Endec.LONG.fieldOf("amount", FluidIngredient::amount),
        FluidIngredient::new);

    public static final FluidIngredient EMPTY = new FluidIngredient();

    public FluidIngredient(Either<TagKey<Fluid>, Identifier> fluidContent, long amount) {
        this.fluidContent = fluidContent;
        this.amount = amount;
    }

    public FluidIngredient() {
        this(Either.right(Registries.FLUID.getId(Fluids.EMPTY)), 0L);
    }

    public FluidIngredient withContent(Identifier fluidId) {
        return new FluidIngredient(Either.right(fluidId), amount);
    }

    public FluidIngredient withContent(RegistryKey<Fluid> fluidKey) {
        return new FluidIngredient(Either.right(fluidKey.getValue()), amount);
    }

    public FluidIngredient withContent(Fluid fluid) {
        return new FluidIngredient(Either.right(Registries.FLUID.getId(fluid)), amount);
    }

    public FluidIngredient withContent(TagKey<Fluid> fluidTag) {
        return new FluidIngredient(Either.left(fluidTag), amount);
    }

    public FluidIngredient withAmount(long withAmount) {
        return new FluidIngredient(fluidContent, withAmount);
    }

    public FluidIngredient withAmount(float withAmount) {
        return new FluidIngredient(fluidContent, (long)(withAmount * FluidStack.bucketAmount()));
    }

    public static FluidIngredient ofStack(FluidStack fluidStack) {
        return new FluidIngredient(
            Either.right(Registries.FLUID.getId(fluidStack.getFluid())),
            fluidStack.getAmount());
    }

    public Text name() {
        return Text.of((Identifier)fluidContent.map(
            tag -> tag.id(),
            id ->  id));
    }

    @Override
    public boolean test(@Nullable FluidStack fluidStack) {
        return matchesFluid(fluidStack) && fluidStack.getAmount() >= this.amount;
    }

    public boolean matchesFluid(Fluid fluid) {
        Registries.FLUID.get(Identifier.of("")).matchesType(fluid);
        return fluidContent.map(
            tag -> Registries.FLUID.getEntry(fluid).isIn(tag),
            id -> Registries.FLUID.get(id).matchesType(fluid));
    }

    public boolean matchesFluid(@Nullable FluidStack fluidStack) {
        return fluidStack != null && matchesFluid(fluidStack.getFluid());
    }

    // Intended for recipe viewer plugins
    public List<FluidStack> getFluidStacks() {
        return (List<FluidStack>)fluidContent.map(
            tag -> Registries.FLUID.streamEntries()
                .filter(fluidEntry -> fluidEntry.isIn(tag))
                .map(fluidEntry -> FluidStack.create(fluidEntry.value(), amount))
                .collect(Collectors.toList()),
            id -> List.of(FluidStack.create(Registries.FLUID.get(fluidContent.right().get()), amount)));
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Override
    public String toString() {
        return "FluidIngredient{" +
                 "fluidContent={" +
                    "tag=" + fluidContent.left() +
                    ", id=" + fluidContent.right() + "}" +
                 ", amount=" + amount +
                 '}';
    }
}
