package rearth.oritech.spaceage.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;

/**
 * Two unordered physical blocks become two identical result states at the source cells.
 */
public record VacuumRecipe(BlockIngredient first, BlockIngredient second, BlockState resultState, int ticks,
                           int rfPerTick)
        implements Recipe<VacuumRecipe.Input> {

    public static final MapCodec<VacuumRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockIngredient.CODEC.fieldOf("first").forGetter(VacuumRecipe::first),
            BlockIngredient.CODEC.fieldOf("second").forGetter(VacuumRecipe::second),
            BlockState.CODEC.fieldOf("result").forGetter(VacuumRecipe::resultState),
            Codec.INT.fieldOf("ticks").forGetter(VacuumRecipe::ticks),
            Codec.INT.fieldOf("rf_per_tick").forGetter(VacuumRecipe::rfPerTick)
    ).apply(i, VacuumRecipe::new));

    public static RecipeSerializer<VacuumRecipe> serializer() {

        return new RecipeSerializer<>(CODEC, ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()));
    }

    public boolean matches(BlockState a, BlockState b) {

        return first.test(a) && second.test(b) || first.test(b) && second.test(a);
    }

    public ItemStack result() {

        return new ItemStack(resultState.getBlock(), 2);
    }

    @Override
    public boolean matches(Input input, Level level) {

        return matches(input.first(), input.second());
    }

    @Override
    public ItemStack assemble(Input input) {

        return result();
    }

    @Override
    public boolean isSpecial() {

        return true;
    }

    @Override
    public boolean showNotification() {

        return false;
    }

    @Override
    public String group() {

        return "";
    }

    @Override
    public PlacementInfo placementInfo() {

        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {

        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeType<VacuumRecipe> getType() {

        return SpaceAgeRecipes.VACUUM_CRAFTING.get();
    }

    @Override
    public RecipeSerializer<VacuumRecipe> getSerializer() {

        return SpaceAgeRecipes.VACUUM_SERIALIZER.get();
    }

    public record Input(BlockState first, BlockState second) implements RecipeInput {

        @Override
        public int size() {

            return 2;
        }

        @Override
        public ItemStack getItem(int index) {

            return new ItemStack((index == 0 ? first : second).getBlock());
        }
    }
}
