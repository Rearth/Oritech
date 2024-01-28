package rearth.oritech.init.recipes;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class RecipeContent {

    public static final OritechRecipe.OritechRecipeType PULVERIZER = register(new Identifier(Oritech.MOD_ID, "pulverizer"));
    public static final OritechRecipe.OritechRecipeType GRINDER = register(new Identifier(Oritech.MOD_ID, "grinder"));

    private static OritechRecipe.OritechRecipeType register(Identifier name) {

        var type = new OritechRecipe.OritechRecipeType(name);

        Registry.register(Registries.RECIPE_TYPE, name, type);
        Registry.register(Registries.RECIPE_SERIALIZER, name, type);

        return type;
    }

    public static void initialize() {
        Oritech.LOGGER.info("Adding oritech mod recipe types");
    }
}
