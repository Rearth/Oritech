package rearth.oritech.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;

import java.util.function.Predicate;

// this is very much inspired by TR: https://github.com/TechReborn/TechReborn/blob/1.21/RebornCore/src/main/java/reborncore/common/crafting/SizedIngredient.java#L50
public record SizedIngredient(int count, Ingredient ingredient) implements Predicate<ItemStack> {
    
    public static MapCodec<SizedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count),
      Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient)
    ).apply(instance, SizedIngredient::new));
    
    public static PacketCodec<RegistryByteBuf, SizedIngredient> PACKET_CODEC = PacketCodec.tuple(
      PacketCodecs.INTEGER, SizedIngredient::count,
      Ingredient.PACKET_CODEC, SizedIngredient::ingredient,
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
