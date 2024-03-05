package rearth.oritech.init.recipes;

import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.compat.OritechREIPlugin;
import rearth.oritech.init.compat.Screens.BasicMachineScreen;
import rearth.oritech.init.compat.Screens.PulverizerScreen;

import java.util.function.BiFunction;

public class RecipeContent {

    public static final OritechRecipeType PULVERIZER = register(new Identifier(Oritech.MOD_ID, "pulverizer"));
    public static final OritechRecipeType GRINDER = register(new Identifier(Oritech.MOD_ID, "grinder"));
    public static final OritechRecipeType ASSEMBLER = register(new Identifier(Oritech.MOD_ID, "assembler"));
    public static final OritechRecipeType FOUNDRY = register(new Identifier(Oritech.MOD_ID, "foundry"));
    public static final OritechRecipeType CENTRIFUGE = register(new Identifier(Oritech.MOD_ID, "centrifuge"));

    private static OritechRecipeType register(Identifier name) {

        var type = new OritechRecipeType(name);

        Registry.register(Registries.RECIPE_TYPE, name, type);
        Registry.register(Registries.RECIPE_SERIALIZER, name, type);

        return type;
    }

    public static void initialize() {
        Oritech.LOGGER.info("Adding oritech mod recipe types");
    }
}
