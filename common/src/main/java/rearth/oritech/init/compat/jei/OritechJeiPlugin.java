package rearth.oritech.init.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.generators.BioGeneratorEntity;
import rearth.oritech.block.entity.generators.FuelGeneratorEntity;
import rearth.oritech.block.entity.generators.LavaGeneratorEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.block.entity.processing.*;
import rearth.oritech.client.ui.BasicMachineScreen;
import rearth.oritech.client.ui.ItemFilterScreen;
import rearth.oritech.client.ui.PlayerModifierScreen;
import rearth.oritech.client.ui.ReactorScreen;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

@JeiPlugin
public class OritechJeiPlugin implements IModPlugin {
    
    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return Oritech.id("jei_plugin");
    }
    
    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        
        registerOritechCategory(registration, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK, PulverizerBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK, FragmentForgeBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK, AssemblerBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK, FoundryBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.REFINERY, BlockContent.REFINERY_BLOCK, RefineryBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.COOLER, BlockContent.COOLER_BLOCK, CoolerBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE_BLOCK, CentrifugeBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK, AtomicForgeBlockEntity.class);
        
        // generators
        registerOritechCategory(registration, RecipeContent.BIO_GENERATOR, BlockContent.BIO_GENERATOR_BLOCK, BioGeneratorEntity.class);
        registerOritechCategory(registration, RecipeContent.FUEL_GENERATOR, BlockContent.FUEL_GENERATOR_BLOCK, FuelGeneratorEntity.class);
        registerOritechCategory(registration, RecipeContent.LAVA_GENERATOR, BlockContent.LAVA_GENERATOR_BLOCK, LavaGeneratorEntity.class);
        registerOritechCategory(registration, RecipeContent.STEAM_ENGINE, BlockContent.STEAM_ENGINE_BLOCK, SteamEngineEntity.class);
        
        // reactor
        registerCustom(registration, RecipeContent.REACTOR, BlockContent.REACTOR_CONTROLLER, true, List.of(new ScreenProvider.GuiSlot(0, 55, 35)), new InventorySlotAssignment(0, 1, 1, 0));
        
        registration.addRecipeCategories(new OritechJeiParticleCollisionRecipe(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new OritechJeiLaserRecipe(registration.getJeiHelpers().getGuiHelper()));
        
    }
    
    private void registerOritechCategory(IRecipeCategoryRegistration registration, OritechRecipeType type, Block block, Class<? extends MachineBlockEntity> machineClass) {
        registration.addRecipeCategories(
          new OritechJeiRecipeCategory(type, machineClass, block, registration.getJeiHelpers().getGuiHelper()));
    }
    
    private void registerCustom(IRecipeCategoryRegistration registration, OritechRecipeType type, Block block, Boolean isGenerator, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment assignments) {
        registration.addRecipeCategories(
          new OritechJeiRecipeCategory(type, block, registration.getJeiHelpers().getGuiHelper(), isGenerator, slots, assignments));
    }
    
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        
        registerRecipe(registration, RecipeContent.PULVERIZER);
        registerRecipe(registration, RecipeContent.GRINDER);
        registerRecipe(registration, RecipeContent.ASSEMBLER);
        registerRecipe(registration, RecipeContent.FOUNDRY);
        registerRecipe(registration, RecipeContent.REFINERY);
        registerRecipe(registration, RecipeContent.COOLER);
        registerRecipe(registration, RecipeContent.CENTRIFUGE);
        registerRecipe(registration, RecipeContent.CENTRIFUGE_FLUID);
        registerRecipe(registration, RecipeContent.ATOMIC_FORGE);
        
        // generators
        registerRecipe(registration, RecipeContent.BIO_GENERATOR);
        registerRecipe(registration, RecipeContent.FUEL_GENERATOR);
        registerRecipe(registration, RecipeContent.LAVA_GENERATOR);
        registerRecipe(registration, RecipeContent.STEAM_ENGINE);
        
        registerRecipe(registration, RecipeContent.PARTICLE_COLLISION);
        registerRecipe(registration, RecipeContent.LASER);
        registerRecipe(registration, RecipeContent.REACTOR);
        
    }
    
    public void registerRecipe(IRecipeRegistration registration, OritechRecipeType type) {
        // this feels incredibly hacky, but seems to be the way to go?
        var world = Minecraft.getInstance().level;
        var data = world.getRecipeManager().getAllRecipesFor(type).stream().map(RecipeHolder::value).toList();
        registration.addRecipes(RecipeType.create(type.getIdentifier().getNamespace(), type.getIdentifier().getPath(), OritechRecipe.class), data);
    }
    
    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        
        registerCatalyst(registration, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK);
        registerCatalyst(registration, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK);
        registerCatalyst(registration, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK);
        registerCatalyst(registration, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK);
        registerCatalyst(registration, RecipeContent.REFINERY, BlockContent.REFINERY_BLOCK);
        registerCatalyst(registration, RecipeContent.COOLER, BlockContent.COOLER_BLOCK);
        registerCatalyst(registration, RecipeContent.CENTRIFUGE, BlockContent.CENTRIFUGE_BLOCK);
        registerCatalyst(registration, RecipeContent.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE_BLOCK);
        registerCatalyst(registration, RecipeContent.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE_BLOCK);
        
        // generators
        registerCatalyst(registration, RecipeContent.BIO_GENERATOR, BlockContent.BIO_GENERATOR_BLOCK);
        registerCatalyst(registration, RecipeContent.FUEL_GENERATOR, BlockContent.FUEL_GENERATOR_BLOCK);
        registerCatalyst(registration, RecipeContent.LAVA_GENERATOR, BlockContent.LAVA_GENERATOR_BLOCK);
        registerCatalyst(registration, RecipeContent.STEAM_ENGINE, BlockContent.STEAM_ENGINE_BLOCK);
        
        registerCatalyst(registration, RecipeContent.PARTICLE_COLLISION, BlockContent.ACCELERATOR_CONTROLLER);
        registerCatalyst(registration, RecipeContent.LASER, BlockContent.LASER_ARM_BLOCK);
        registerCatalyst(registration, RecipeContent.REACTOR, BlockContent.REACTOR_CONTROLLER);
    }
    
    private void registerCatalyst(IRecipeCatalystRegistration registration, OritechRecipeType type, Block block) {
        registration.addRecipeCatalyst(block, RecipeType.create(type.getIdentifier().getNamespace(), type.getIdentifier().getPath(), OritechRecipe.class));
    }
    
    @Override
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
        IModPlugin.super.registerGuiHandlers(registration);
        
        registration.addGenericGuiContainerHandler(BasicMachineScreen.class, new JeiExclusionZoneHandler());
        registration.addGenericGuiContainerHandler(ReactorScreen.class, new JeiExclusionZoneHandler());
        registration.addGenericGuiContainerHandler(PlayerModifierScreen.class, new JeiExclusionZoneHandler());

        registration.addGhostIngredientHandler(ItemFilterScreen.class, new JeiItemFilterGhostHandler());
    }

}