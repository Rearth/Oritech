package rearth.oritech.util;

import com.mojang.datafixers.util.Either;
import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Inspired by Immersive Engineering https://github.com/BluSunrize/ImmersiveEngineering/blob/1.21.1/src/api/java/blusunrize/immersiveengineering/api/crafting/FluidTagInput.java

/**
 * A FluidIngredient can be either a fluid Identifier or a fluid TagKey
 * <p>
 * Used for input to recipes. The amount defaults to 1 bucket.
 */
public record FluidIngredient(Either<TagKey<Fluid>, Identifier> fluidContent,
                              long amount) implements Predicate<FluidStack> {
    
    // A FluidIngredient should have a "fluid" which can be an identifier with a namespace or a tag beginning in #
    // A FluidIngredient can have an "amount" should be a long integer and will default to 1 bucket
    public static final Endec<FluidIngredient> FLUID_INGREDIENT_ENDEC = StructEndecBuilder.of(
      CodecUtils.eitherEndec(
        Endec.STRING.xmap(
          s -> {
              if (s.charAt(0) != '#') throw new IllegalStateException("tag must start with #");
              return TagKey.of(RegistryKeys.FLUID, Identifier.of(s.substring(1)));
          },
          tag -> "#" + tag.id()
        ),
        MinecraftEndecs.IDENTIFIER
      ).fieldOf("fluid", FluidIngredient::fluidContent),
      Endec.LONG.optionalFieldOf("amount", FluidIngredient::amount, FluidStack.bucketAmount()),
      FluidIngredient::new
    );
    
    public static final PacketCodec<ByteBuf, TagKey<Fluid>> FLUID_TAG_KEY_CODEC = Identifier.PACKET_CODEC.xmap(id -> TagKey.of(RegistryKeys.FLUID, id), TagKey::id);
    
    public static final PacketCodec<RegistryByteBuf, Either<TagKey<Fluid>, Identifier>> FLUID_CONTENT_CODEC =
      PacketCodecs.either(FLUID_TAG_KEY_CODEC, Identifier.PACKET_CODEC);
    
    public static final PacketCodec<RegistryByteBuf, FluidIngredient> PACKET_CODEC =
      PacketCodec.tuple(
        FLUID_CONTENT_CODEC, FluidIngredient::fluidContent,
        PacketCodecs.VAR_LONG, FluidIngredient::amount,
        FluidIngredient::new
      );
    
    public static final FluidIngredient EMPTY = new FluidIngredient();
    
    public FluidIngredient(Either<TagKey<Fluid>, Identifier> fluidContent, long amount) {
        this.fluidContent = fluidContent;
        this.amount = amount;
    }
    
    // Construct EMPTY FluidIngredient
    public FluidIngredient() {
        this(Either.right(Registries.FLUID.getId(Fluids.EMPTY)), 0L);
    }
    
    // All with* methods will return a copy of the record with updated fluid content
    // If the fluid amount is zero when a non-empty fluid is set, it will default the amount to 1 bucket
    public FluidIngredient withContent(Identifier fluidId) {
        return new FluidIngredient(
          Either.right(fluidId),
          (amount == 0 && Registries.FLUID.get(fluidId) != Fluids.EMPTY) ? FluidStack.bucketAmount() : amount);
    }
    
    public FluidIngredient withContent(RegistryKey<Fluid> fluidKey) {
        return withContent(fluidKey.getValue());
    }
    
    public FluidIngredient withContent(Fluid fluid) {
        return withContent(Registries.FLUID.getId(fluid));
    }
    
    public FluidIngredient withContent(TagKey<Fluid> fluidTag) {
        // even if the tag is empty now, it might not be later.
        // don't test for empty tag at this point. If a tag might be empty, it would be better
        // to add conditional loading for the recipes that use it.
        return new FluidIngredient(Either.left(fluidTag), amount == 0 ? FluidStack.bucketAmount() : amount);
    }
    
    public FluidIngredient withAmount(long withAmount) {
        return new FluidIngredient(fluidContent, withAmount);
    }
    
    public FluidIngredient withAmount(float withAmountInBuckets) {
        return new FluidIngredient(fluidContent, (long) (withAmountInBuckets * FluidStack.bucketAmount()));
    }
    
    public FluidIngredient withSpecificAmount(long amountInMillis) {
        return new FluidIngredient(fluidContent, amountInMillis);
    }
    
    public static FluidIngredient ofStack(FluidStack fluidStack) {
        return new FluidIngredient(Either.right(Registries.FLUID.getId(fluidStack.getFluid())), fluidStack.getAmount());
    }
    
    public Text name() {
        Identifier fluidId = fluidContent.map(tag -> tag.id(), id -> id);
        return hasTag()
                 ? Text.of("#" + fluidId.getNamespace() + ":" + fluidId.getPath())
                 : Text.translatable("fluid." + fluidContent.map(tag -> tag.id(), id -> id).toTranslationKey());
    }
    
    @Override
    public boolean test(@Nullable FluidStack fluidStack) {
        return matchesFluid(fluidStack) && fluidStack.getAmount() >= this.amount;
    }
    
    public boolean matchesFluid(Fluid fluid) {
        Registries.FLUID.get(Identifier.of("")).matchesType(fluid);
        return fluidContent.map(tag -> Registries.FLUID.getEntry(fluid).isIn(tag), id -> Registries.FLUID.get(id).matchesType(fluid));
    }
    
    public boolean matchesFluid(@Nullable FluidStack fluidStack) {
        return fluidStack != null && matchesFluid(fluidStack.getFluid());
    }
    
    // Intended for recipe viewer plugins
    public List<FluidStack> getFluidStacks() {
        return (List<FluidStack>) fluidContent.map(
          tag -> Registries.FLUID.streamEntries().filter(fluidEntry -> fluidEntry.isIn(tag)).map(fluidEntry -> FluidStack.create(fluidEntry.value(), amount)).collect(Collectors.toList()),
          id -> List.of(FluidStack.create(Registries.FLUID.get(fluidContent.right().get()), amount))
        );
    }
    
    public boolean isEmpty() {
        return this == EMPTY;
    }
    
    public boolean hasTag() {
        return fluidContent.left().isPresent();
    }
    
    // mostly for convenience in recipe viewers
    // make sure this is a tag calling, otherwise it could throw an exception
    public TagKey<Fluid> getTag() {
        return fluidContent.left().get();
    }
    
    // mostly for convenience in recipe viewers
    // make sure this isn't a tag before calling, otherwise it could throw an exception
    public Fluid getFluid() {
        return Registries.FLUID.get(fluidContent.right().get());
    }
    
    @Override
    public String toString() {
        return "FluidIngredient{" + "fluidContent={" + "tag=" + fluidContent.left() + ", id=" + fluidContent.right() + "}" + ", amount=" + amount + '}';
    }
}
