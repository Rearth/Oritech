package rearth.oritech.init.compat;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.block.Block;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.compat.Screens.BasicMachineScreen;
import rearth.oritech.init.compat.Screens.PulverizerScreen;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.HashSet;
import java.util.function.BiFunction;

public class OritechREIPlugin implements REIClientPlugin {
    
    public record OriREICategory(OritechRecipeType type, ItemConvertible icon, BiFunction<OritechRecipeType, ItemConvertible, BasicMachineScreen> screenType) {}
    public static final HashSet<OriREICategory> categoriesToRegister = new HashSet<>();
    
    public record OriREIWorkstation(String identifier, ItemConvertible machine) {}
    public static final HashSet<OriREIWorkstation> workstationsToRegister = new HashSet<>();
    
    @Override
    public void registerCategories(CategoryRegistry registry) {
        
        for(var entry : categoriesToRegister) {
            registerOritechCategory(registry, entry.type, entry.icon, entry.screenType);
        }
        
        for (var entry : workstationsToRegister) {
            registerOriWorkstation(registry, getTypeByIdentifier(entry.identifier), entry.machine);
        }
    }
    
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private OritechRecipeType getTypeByIdentifier(String path) {
        var result = categoriesToRegister.stream().findFirst().get().type;
        
        for (var entry : categoriesToRegister) {
            if (entry.type.getIdentifier().equals(new Identifier(Oritech.MOD_ID, path)))
                return entry.type;
        }
        
        Oritech.LOGGER.error("Unable to find recipe type: " + path);
        
        return result;
    }
    
    // creates a screen instance that displays all recipes of that recipe type
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (var entry : categoriesToRegister) {
            registerMachineRecipeType(registry, entry.type);
        }
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
