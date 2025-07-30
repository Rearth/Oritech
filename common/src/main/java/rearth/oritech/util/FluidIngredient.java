package rearth.oritech.util;

import com.mojang.datafixers.util.Either;
import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

// Inspired by Immersive Engineering https://github.com/BluSunrize/ImmersiveEngineering/blob/1.21.1/src/api/java/blusunrize/immersiveengineering/api/crafting/FluidTagInput.java

/**
 * A FluidIngredient can be either a fluid Identifier or a fluid TagKey
 * <p>
 * Used for input to recipes. The amount defaults to 1 bucket.
 */
public record FluidIngredient(Either<TagKey<Fluid>, ResourceLocation> fluidContent,
                              long amount) implements Predicate<FluidStack> {
    
    // A FluidIngredient should have a "fluid" which can be an identifier with a namespace or a tag beginning in #
    // A FluidIngredient can have an "amount" should be a long integer and will default to 1 bucket
    public static final Endec<FluidIngredient> FLUID_INGREDIENT_ENDEC = StructEndecBuilder.of(
      CodecUtils.eitherEndec(
        Endec.STRING.xmap(
          s -> {
              if (s.charAt(0) != '#') throw new IllegalStateException("tag must start with #");
              return TagKey.create(Registries.FLUID, ResourceLocation.parse(s.substring(1)));
          },
          tag -> "#" + tag.location()
        ),
        MinecraftEndecs.IDENTIFIER
      ).fieldOf("fluid", FluidIngredient::fluidContent),
      Endec.LONG.optionalFieldOf("amount", FluidIngredient::amount, FluidStack.bucketAmount()),
      FluidIngredient::new
    );
    
    public static final StreamCodec<ByteBuf, TagKey<Fluid>> FLUID_TAG_KEY_CODEC = ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.FLUID, id), TagKey::location);
    
    public static final StreamCodec<RegistryFriendlyByteBuf, Either<TagKey<Fluid>, ResourceLocation>> FLUID_CONTENT_CODEC =
      ByteBufCodecs.either(FLUID_TAG_KEY_CODEC, ResourceLocation.STREAM_CODEC);
    
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> PACKET_CODEC =
      StreamCodec.composite(
        FLUID_CONTENT_CODEC, FluidIngredient::fluidContent,
        ByteBufCodecs.VAR_LONG, FluidIngredient::amount,
        FluidIngredient::new
      );
    
    public static final FluidIngredient EMPTY = new FluidIngredient();
    
    public FluidIngredient(Either<TagKey<Fluid>, ResourceLocation> fluidContent, long amount) {
        this.fluidContent = fluidContent;
        this.amount = amount;
    }
    
    // Construct EMPTY FluidIngredient
    public FluidIngredient() {
        this(Either.right(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY)), 0L);
    }
    
    // All with* methods will return a copy of the record with updated fluid content
    // If the fluid amount is zero when a non-empty fluid is set, it will default the amount to 1 bucket
    public FluidIngredient withContent(ResourceLocation fluidId) {
        return new FluidIngredient(
          Either.right(fluidId),
          (amount == 0 && BuiltInRegistries.FLUID.get(fluidId) != Fluids.EMPTY) ? FluidStack.bucketAmount() : amount);
    }
    
    public FluidIngredient withContent(ResourceKey<Fluid> fluidKey) {
        return withContent(fluidKey.location());
    }
    
    public FluidIngredient withContent(Fluid fluid) {
        return withContent(BuiltInRegistries.FLUID.getKey(fluid));
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
        return new FluidIngredient(Either.right(BuiltInRegistries.FLUID.getKey(fluidStack.getFluid())), fluidStack.getAmount());
    }
    
    public Component name() {
        ResourceLocation fluidId = fluidContent.map(tag -> tag.location(), id -> id);
        return hasTag()
                 ? Component.nullToEmpty("#" + fluidId.getNamespace() + ":" + fluidId.getPath())
                 : Component.translatable("fluid." + fluidContent.map(tag -> tag.location(), id -> id).toLanguageKey());
    }
    
    @Override
    public boolean test(@Nullable FluidStack fluidStack) {
        return matchesFluid(fluidStack) && fluidStack.getAmount() >= this.amount;
    }
    
    public boolean matchesFluid(Fluid fluid) {
        BuiltInRegistries.FLUID.get(ResourceLocation.parse("")).isSame(fluid);
        return fluidContent.map(tag -> BuiltInRegistries.FLUID.wrapAsHolder(fluid).is(tag), id -> BuiltInRegistries.FLUID.get(id).isSame(fluid));
    }
    
    public boolean matchesFluid(@Nullable FluidStack fluidStack) {
        return fluidStack != null && matchesFluid(fluidStack.getFluid());
    }
    
    // Intended for recipe viewer plugins
    public List<FluidStack> getFluidStacks() {
        return (List<FluidStack>) fluidContent.map(
          tag -> BuiltInRegistries.FLUID.holders().filter(fluidEntry -> fluidEntry.is(tag)).map(fluidEntry -> FluidStack.create(fluidEntry.value(), amount)).collect(Collectors.toList()),
          id -> List.of(FluidStack.create(BuiltInRegistries.FLUID.get(fluidContent.right().get()), amount))
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
        return BuiltInRegistries.FLUID.get(fluidContent.right().get());
    }
    
    @Override
    public String toString() {
        return "FluidIngredient{" + "fluidContent={" + "tag=" + fluidContent.left() + ", id=" + fluidContent.right() + "}" + ", amount=" + amount + '}';
    }
}
