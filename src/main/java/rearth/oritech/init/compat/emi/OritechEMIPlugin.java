package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.RecipeManager;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.machines.generators.BioGeneratorEntity;
import rearth.oritech.block.entity.machines.generators.FuelGeneratorEntity;
import rearth.oritech.block.entity.machines.generators.LavaGeneratorEntity;
import rearth.oritech.block.entity.machines.generators.SteamEngineEntity;
import rearth.oritech.block.entity.machines.processing.*;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;

public class OritechEMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        
        var manager = registry.getRecipeManager();
        
        registerOritechCategory(registry, manager, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK, PulverizerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK, FragmentForgeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK, AssemblerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK, FoundryBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK, AtomicForgeBlockEntity.class);
        
        // generators
        registerOritechCategory(registry, manager, RecipeContent.BIO_GENERATOR, BlockContent.BIO_GENERATOR_BLOCK, BioGeneratorEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.FUEL_GENERATOR, BlockContent.FUEL_GENERATOR_BLOCK, FuelGeneratorEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.LAVA_GENERATOR, BlockContent.LAVA_GENERATOR_BLOCK, LavaGeneratorEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.STEAM_ENGINE, BlockContent.STEAM_ENGINE_BLOCK, SteamEngineEntity.class);
        
        // others
        registerParticleAccelerator(registry, manager, RecipeContent.PARTICLE_COLLISION);
        
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(BlockContent.POWERED_FURNACE_BLOCK));
        
    }
    
    private void registerOritechCategory(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType, ItemConvertible machine,  Class<? extends MachineBlockEntity> screenProviderSource) {
        var icon = EmiStack.of(machine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        
        var blockState = Blocks.STONE.getDefaultState();
        if (machine instanceof Block blockItem)
            blockState = blockItem.getDefaultState();
        var finalBlockState = blockState;
        
        manager.listAllOfType(recipeType)
          .stream()
          .map(entry -> new OritechEMIRecipe(entry, category, screenProviderSource, finalBlockState))
          .forEach(registry::addRecipe);
        
    }
    
    private void registerParticleAccelerator(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType) {
        
        var machine = BlockContent.ACCELERATOR_CONTROLLER;
        
        var icon = EmiStack.of(machine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        
        manager.listAllOfType(recipeType)
          .stream()
          .map(entry -> new OritechEMIParticleCollisionRecipe(entry, category))
          .forEach(registry::addRecipe);
        
    }
}
