package rearth.oritech.init.compat;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemConvertible;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.compat.Screens.BasicMachineScreen;
import rearth.oritech.init.compat.Screens.PulverizerScreen;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.function.BiFunction;

public class OritechREIPlugin implements REIClientPlugin {
    
    @Override
    public void registerCategories(CategoryRegistry registry) {
        
        // recipe types
        registerOritechCategory(registry, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK, PulverizerScreen::new);
        registerOritechCategory(registry, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK, BasicMachineScreen::new);
        registerOritechCategory(registry, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK, BasicMachineScreen::new);
        registerOritechCategory(registry, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK, BasicMachineScreen::new);
        registerOritechCategory(registry, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK, BasicMachineScreen::new);
        registerOritechCategory(registry, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK, BasicMachineScreen::new);
        
        // workstations
        registerOriWorkstation(registry, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK);
        registerOriWorkstation(registry, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK);
        registerOriWorkstation(registry, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK);
        registerOriWorkstation(registry, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK);
        registerOriWorkstation(registry, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK);
        registerOriWorkstation(registry, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK);
        
        registry.addWorkstations(CategoryIdentifier.of("minecraft", "plugins/smelting"), EntryStacks.of(BlockContent.POWERED_FURNACE_BLOCK));
    }
    
    // creates a screen instance that displays all recipes of that recipe type
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        
        // recipes again
        registerMachineRecipeType(registry, RecipeContent.PULVERIZER);
        registerMachineRecipeType(registry, RecipeContent.ASSEMBLER);
        registerMachineRecipeType(registry, RecipeContent.GRINDER);
        registerMachineRecipeType(registry, RecipeContent.FOUNDRY);
        registerMachineRecipeType(registry, RecipeContent.CENTRIFUGE);
        registerMachineRecipeType(registry, RecipeContent.ATOMIC_FORGE);
    }
    
    private void registerOritechCategory(CategoryRegistry registry, OritechRecipeType recipeType, ItemConvertible machineIcon, BiFunction<OritechRecipeType, ItemConvertible, BasicMachineScreen> screenType) {
        var oriCategory = screenType.apply(recipeType, machineIcon);
        registry.add(oriCategory);
    }
    
    private void registerOriWorkstation(CategoryRegistry registry, OritechRecipeType recipeType, ItemConvertible machine) {
        registry.addWorkstations(CategoryIdentifier.of(recipeType.getIdentifier()), EntryStacks.of(machine));
    }
    
    private void registerMachineRecipeType(DisplayRegistry registry, OritechRecipeType recipeType) {
        registry.registerRecipeFiller(OritechRecipe.class, recipeType, OritechDisplay::new);
    }
}
