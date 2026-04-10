package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.generators.BioGeneratorEntity;
import rearth.oritech.block.entity.generators.FuelGeneratorEntity;
import rearth.oritech.block.entity.generators.LavaGeneratorEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.block.entity.processing.*;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.ItemFilterScreen;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;
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
        
        registerOritechCategory(registry, manager, RecipeContent.PULVERIZER, PulverizerBlockEntity.class, BlockContent.PULVERIZER_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.GRINDER, FragmentForgeBlockEntity.class, BlockContent.FRAGMENT_FORGE_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.ASSEMBLER, AssemblerBlockEntity.class, BlockContent.ASSEMBLER_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.FOUNDRY, FoundryBlockEntity.class, BlockContent.FOUNDRY_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.COOLER, CoolerBlockEntity.class, BlockContent.COOLER_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE, CentrifugeBlockEntity.class, BlockContent.CENTRIFUGE_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.CENTRIFUGE_FLUID, CentrifugeBlockEntity.class, BlockContent.CENTRIFUGE_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.ATOMIC_FORGE, AtomicForgeBlockEntity.class, BlockContent.ATOMIC_FORGE_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.REFINERY, RefineryBlockEntity.class, BlockContent.REFINERY_BLOCK, BlockContent.TAINTED_REFINERY_BLOCK);
        
        // tainted refinery also processes refinery recipes
        var refineryCategory = new EmiRecipeCategory(RecipeContent.REFINERY.getIdentifier(), EmiStack.of(BlockContent.REFINERY_BLOCK));
        registry.addWorkstation(refineryCategory, EmiStack.of(BlockContent.TAINTED_REFINERY_BLOCK));
        
        // generators
        registerOritechCategory(registry, manager, RecipeContent.BIO_GENERATOR, BioGeneratorEntity.class, BlockContent.BIO_GENERATOR_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.FUEL_GENERATOR, FuelGeneratorEntity.class, BlockContent.FUEL_GENERATOR_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.LAVA_GENERATOR, LavaGeneratorEntity.class, BlockContent.LAVA_GENERATOR_BLOCK);
        registerOritechCategory(registry, manager, RecipeContent.STEAM_ENGINE, SteamEngineEntity.class, BlockContent.STEAM_ENGINE_BLOCK);
        
        // reactor
        registerCustom(registry, manager, RecipeContent.REACTOR, BlockContent.REACTOR_CONTROLLER, List.of(new ScreenProvider.GuiSlot(0, 55, 35)), new InventorySlotAssignment(0, 1, 1, 0));
        
        // others
        registerParticleAccelerator(registry, manager, RecipeContent.PARTICLE_COLLISION);
        registerLaser(registry, manager, RecipeContent.LASER);
        
        // tainted refinery info categories
        registerTaintedRefineryCreation(registry);
        registerTaintedRefineryBonuses(registry);
        
        registry.addWorkstation(VanillaEmiRecipeCategories.SMELTING, EmiStack.of(BlockContent.POWERED_FURNACE_BLOCK));
        
        registry.addRecipeHandler(ModScreens.ASSEMBLER_SCREEN, new EmiTransferHandler<>(RecipeContent.ASSEMBLER.getIdentifier()));
        registry.addRecipeHandler(ModScreens.FOUNDRY_SCREEN, new EmiTransferHandler<>(RecipeContent.FOUNDRY.getIdentifier()));
        registry.addRecipeHandler(ModScreens.ATOMIC_FORGE_SCREEN, new EmiTransferHandler<>(RecipeContent.ATOMIC_FORGE.getIdentifier()));
        registry.addRecipeHandler(ModScreens.PULVERIZER_SCREEN, new EmiTransferHandler<>(RecipeContent.PULVERIZER.getIdentifier()));
        registry.addRecipeHandler(ModScreens.GRINDER_SCREEN, new EmiTransferHandler<>(RecipeContent.GRINDER.getIdentifier()));
        registry.addRecipeHandler(ModScreens.COOLER_SCREEN, new EmiTransferHandler<>(RecipeContent.COOLER.getIdentifier()));
        registry.addRecipeHandler(ModScreens.CENTRIFUGE_SCREEN, new EmiTransferHandler<>(RecipeContent.CENTRIFUGE.getIdentifier()));
        registry.addRecipeHandler(ModScreens.CENTRIFUGE_SCREEN, new EmiTransferHandler<>(RecipeContent.CENTRIFUGE_FLUID.getIdentifier()));
        registry.addRecipeHandler(ModScreens.REFINERY_SCREEN, new EmiTransferHandler<>(RecipeContent.REFINERY.getIdentifier()));

        registry.addDragDropHandler(ItemFilterScreen.class, new EmiItemFilterDragDropHandler());
        
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (!(screen instanceof OritechWidgetScreen<?> oritechScreen)) return;
            
            oritechScreen.getExclusionZones().forEach(elem -> consumer.accept(new Bounds(elem.getX(), elem.getY(), elem.getWidth(), elem.getHeight())));
        });
        
    }
    
    private void registerOritechCategory(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType, Class<? extends MachineBlockEntity> screenProviderSource, ItemLike... machines) {
        
        var firstMachine = machines[0];
        var icon = EmiStack.of(firstMachine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        for (var machine : machines) {
            registry.addWorkstation(category, EmiStack.of(machine));
        }
        
        var blockState = Blocks.STONE.defaultBlockState();
        if (firstMachine instanceof Block blockItem)
            blockState = blockItem.defaultBlockState();
        var finalBlockState = blockState;
        
        manager.getAllRecipesFor(recipeType)
          .stream()
          .map(entry -> new OritechEMIRecipe(entry, category, screenProviderSource, finalBlockState))
          .forEach(registry::addRecipe);
        
    }
    
    private void registerCustom(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType, ItemLike machine, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment assignments) {
        
        var icon = EmiStack.of(machine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        
        manager.getAllRecipesFor(recipeType)
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
        
        manager.getAllRecipesFor(recipeType)
          .stream()
          .map(entry -> new OritechEMIParticleCollisionRecipe(entry, category))
          .forEach(registry::addRecipe);
        
    }
    
    private void registerTaintedRefineryCreation(EmiRegistry registry) {
        var icon = EmiStack.of(BlockContent.TAINTED_REFINERY_BLOCK);
        var id = Oritech.id("tainted_refinery_creation");
        var category = new EmiRecipeCategory(id, icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, EmiStack.of(BlockContent.REFINERY_BLOCK));
        registry.addWorkstation(category, EmiStack.of(BlockContent.ENCHANTMENT_CATALYST_BLOCK));
        registry.addRecipe(new OritechEmiTaintedRefineryCreation(category));
    }
    
    private void registerTaintedRefineryBonuses(EmiRegistry registry) {
        var icon = EmiStack.of(BlockContent.TAINTED_REFINERY_BLOCK);
        var id = Oritech.id("tainted_refinery_bonuses");
        var category = new EmiRecipeCategory(id, icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        registry.addRecipe(new OritechEmiTaintedRefineryBonuses(category, TagContent.REFINERY_SCULK_BLOCKS, "sculk", "emi.description.oritech.tainted_bonus.sculk"));
        registry.addRecipe(new OritechEmiTaintedRefineryBonuses(category, TagContent.REFINERY_ARCANE_BLOCKS, "arcane", "emi.description.oritech.tainted_bonus.arcane"));
    }
    
    private void registerLaser(EmiRegistry registry, RecipeManager manager, OritechRecipeType recipeType) {
        
        var machine = BlockContent.LASER_ARM_BLOCK;
        
        var icon = EmiStack.of(machine);
        var category = new EmiRecipeCategory(recipeType.getIdentifier(), icon);
        
        registry.addCategory(category);
        registry.addWorkstation(category, icon);
        
        manager.getAllRecipesFor(recipeType)
          .stream()
          .map(entry -> new OritechEmiLaserRecipe(entry, category))
          .forEach(registry::addRecipe);
        
    }
}
