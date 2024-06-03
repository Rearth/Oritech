package rearth.oritech.init.recipes;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class RecipeContent {

    public static final OritechRecipeType PULVERIZER = register(new Identifier(Oritech.MOD_ID, "pulverizer"));
    public static final OritechRecipeType GRINDER = register(new Identifier(Oritech.MOD_ID, "grinder"));
    public static final OritechRecipeType ASSEMBLER = register(new Identifier(Oritech.MOD_ID, "assembler"));
    public static final OritechRecipeType FOUNDRY = register(new Identifier(Oritech.MOD_ID, "foundry"));
    public static final OritechRecipeType CENTRIFUGE = register(new Identifier(Oritech.MOD_ID, "centrifuge"));
    public static final OritechRecipeType CENTRIFUGE_FLUID = register(new Identifier(Oritech.MOD_ID, "centrifuge_fluid"));
    public static final OritechRecipeType ATOMIC_FORGE = register(new Identifier(Oritech.MOD_ID, "atomic_forge"));
    public static final OritechRecipeType BIO_GENERATOR = register(new Identifier(Oritech.MOD_ID, "bio_generator"));
    public static final OritechRecipeType FUEL_GENERATOR = register(new Identifier(Oritech.MOD_ID, "fuel_generator"));
    public static final OritechRecipeType LAVA_GENERATOR = register(new Identifier(Oritech.MOD_ID, "lava_generator"));
    public static final OritechRecipeType DEEP_DRILL = register(new Identifier(Oritech.MOD_ID, "deep_drill"));

    private static OritechRecipeType register(Identifier name) {

        var type = new OritechRecipeType(name);

        Registry.register(Registries.RECIPE_TYPE, name, type);
        Registry.register(Registries.RECIPE_SERIALIZER, name, type);

        return type;
    }

    public static void initialize() {
        Oritech.LOGGER.debug("Adding oritech mod recipe types");
    }
}
