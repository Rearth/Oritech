package rearth.oritech.spaceage.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Datapacks use a block ID or a #block_tag; no item form is required for matching.
 */
public record BlockIngredient(String value) {

    public static final Codec<BlockIngredient> CODEC = Codec.STRING.xmap(BlockIngredient::new, BlockIngredient::value);

    public BlockIngredient {

        Identifier.parse(value.startsWith("#") ? value.substring(1) : value);
    }

    public static BlockIngredient of(Block block) {

        return new BlockIngredient(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    public boolean test(BlockState state) {

        if (state.isAir()) return false;
        return value.startsWith("#") ? state.is(TagKey.create(Registries.BLOCK, Identifier.parse(value.substring(1))))
                : state.is(BuiltInRegistries.BLOCK.getValue(Identifier.parse(value)));
    }

    public List<ItemStack> display() {

        return BuiltInRegistries.BLOCK.stream().filter(block -> test(block.defaultBlockState()))
                .map(block -> new ItemStack(block)).filter(stack -> !stack.isEmpty()).toList();
    }
}
