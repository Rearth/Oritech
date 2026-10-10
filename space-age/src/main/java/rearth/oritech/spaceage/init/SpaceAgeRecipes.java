package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.function.Supplier;

public final class SpaceAgeRecipes {

    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, OritechSpaceAge.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, OritechSpaceAge.MOD_ID);
    public static final Supplier<RecipeType<VacuumRecipe>> VACUUM_CRAFTING = TYPES.register("vacuum_crafting", () -> new RecipeType<>() {

        @Override
        public String toString() {

            return OritechSpaceAge.id("vacuum_crafting").toString();
        }
    });
    public static final Supplier<RecipeSerializer<VacuumRecipe>> VACUUM_SERIALIZER = SERIALIZERS.register("vacuum_crafting", VacuumRecipe::serializer);

    private SpaceAgeRecipes() {
    }
}
