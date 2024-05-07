package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.RecipeManager;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.machines.processing.AssemblerBlockEntity;
import rearth.oritech.block.entity.machines.processing.AtomicForgeBlockEntity;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.block.entity.machines.processing.FoundryBlockEntity;
import rearth.oritech.block.entity.machines.processing.FragmentForgeBlockEntity;
import rearth.oritech.block.entity.machines.processing.PulverizerBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.compat.emi.recipes.OritechEmiRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;

public class OritechEMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        RecipeManager manager = registry.getRecipeManager();
        registerOritechCategory(registry, manager, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK, PulverizerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK, FragmentForgeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK, AssemblerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK, FoundryBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK, AtomicForgeBlockEntity.class);
        
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(BlockContent.POWERED_FURNACE_BLOCK));
    }

    private void registerOritechCategory(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType, ItemConvertible machine,  Class<? extends MachineBlockEntity> screenProviderSource) {
        var icon = EmiStack.of(machine);
        var oriCategory = new LazyEmiRecipeCategory(recipeType.getIdentifier(), icon);
        registry.addCategory(oriCategory);
        registry.addWorkstation(oriCategory, icon);
        manager.listAllOfType(recipeType)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, oriCategory, screenProviderSource))
                .forEach(registry::addRecipe);
    }
}
