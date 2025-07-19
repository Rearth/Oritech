package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
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
import rearth.oritech.block.entity.generators.BioGeneratorEntity;
import rearth.oritech.block.entity.generators.FuelGeneratorEntity;
import rearth.oritech.block.entity.generators.LavaGeneratorEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.block.entity.processing.*;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.ItemFilterScreen;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

@EmiEntrypoint
public class OritechEMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        
        var manager = registry.getRecipeManager();
        
        registerOritechCategory(registry, manager, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK, PulverizerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK, FragmentForgeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK, AssemblerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK, FoundryBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.COOLER, BlockContent.COOLER_BLOCK, CoolerBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK, AtomicForgeBlockEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.REFINERY, BlockContent.REFINERY_BLOCK, RefineryBlockEntity.class);
        
        // generators
        registerOritechCategory(registry, manager, RecipeContent.BIO_GENERATOR, BlockContent.BIO_GENERATOR_BLOCK, BioGeneratorEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.FUEL_GENERATOR, BlockContent.FUEL_GENERATOR_BLOCK, FuelGeneratorEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.LAVA_GENERATOR, BlockContent.LAVA_GENERATOR_BLOCK, LavaGeneratorEntity.class);
        registerOritechCategory(registry, manager, RecipeContent.STEAM_ENGINE, BlockContent.STEAM_ENGINE_BLOCK, SteamEngineEntity.class);
        
        // reactor
        registerCustom(registry, manager, RecipeContent.REACTOR, BlockContent.REACTOR_CONTROLLER, List.of(new ScreenProvider.GuiSlot(0, 55, 35)), new InventorySlotAssignment(0, 1, 1, 0));
        
        // others
        registerParticleAccelerator(registry, manager, RecipeContent.PARTICLE_COLLISION);
        registerLaser(registry, manager, RecipeContent.LASER);
        
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(BlockContent.POWERED_FURNACE_BLOCK));
        
        registry.addRecipeHandler(ModScreens.ASSEMBLER_SCREEN, new EmiTransferHandler<>(RecipeContent.ASSEMBLER.getIdentifier()));
        registry.addRecipeHandler(ModScreens.FOUNDRY_SCREEN, new EmiTransferHandler<>(RecipeContent.FOUNDRY.getIdentifier()));
        registry.addRecipeHandler(ModScreens.ATOMIC_FORGE_SCREEN, new EmiTransferHandler<>(RecipeContent.ATOMIC_FORGE.getIdentifier()));

        registry.addDragDropHandler(ItemFilterScreen.class, new EmiItemFilterDragDropHandler());
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
    
    private void registerCustom(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType, ItemConvertible machine, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment assignments) {
        
        var icon = EmiStack.of(machine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        
        manager.listAllOfType(recipeType)
          .stream()
          .map(entry -> new OritechEMIRecipe(entry, category, true, slots, assignments))
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
    
    private void registerLaser(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType) {
        
        var machine = BlockContent.LASER_ARM_BLOCK;
        
        var icon = EmiStack.of(machine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        
        manager.listAllOfType(recipeType)
          .stream()
          .map(entry -> new OritechEmiLaserRecipe(entry, category))
          .forEach(registry::addRecipe);
        
    }
}
