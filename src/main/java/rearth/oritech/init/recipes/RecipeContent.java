package rearth.oritech.init.recipes;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class RecipeContent {

    public static final OritechRecipeType PULVERIZER = register(Oritech.id("pulverizer"));
    public static final OritechRecipeType GRINDER = register(Oritech.id("grinder"));
    public static final OritechRecipeType ASSEMBLER = register(Oritech.id("assembler"));
    public static final OritechRecipeType FOUNDRY = register(Oritech.id("foundry"));
    public static final OritechRecipeType CENTRIFUGE = register(Oritech.id("centrifuge"));
    public static final OritechRecipeType CENTRIFUGE_FLUID = register(Oritech.id("centrifuge_fluid"));
    public static final OritechRecipeType ATOMIC_FORGE = register(Oritech.id("atomic_forge"));
    public static final OritechRecipeType BIO_GENERATOR = register(Oritech.id("bio_generator"));
    public static final OritechRecipeType FUEL_GENERATOR = register(Oritech.id("fuel_generator"));
    public static final OritechRecipeType LAVA_GENERATOR = register(Oritech.id("lava_generator"));
    public static final OritechRecipeType DEEP_DRILL = register(Oritech.id("deep_drill"));

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
