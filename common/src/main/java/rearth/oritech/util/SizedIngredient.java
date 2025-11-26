package rearth.oritech.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

// this is very much inspired by TR: https://github.com/TechReborn/TechReborn/blob/1.21/RebornCore/src/main/java/reborncore/common/crafting/SizedIngredient.java#L50
public record SizedIngredient(int count, Ingredient ingredient) implements Predicate<ItemStack> {
    
    public static MapCodec<SizedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count),
      Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(SizedIngredient::ingredient)
    ).apply(instance, SizedIngredient::new));
    
    public static StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, SizedIngredient::count,
      Ingredient.CONTENTS_STREAM_CODEC, SizedIngredient::ingredient,
      SizedIngredient::new
    );
    
    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            // Never match empty stacks.
            return false;
        }
        
        if (itemStack.getCount() < count) {
            return false;
        }
        
        return ingredient.test(itemStack);
    }
}
