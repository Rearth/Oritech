package rearth.oritech.init.compat.jei;

import io.wispforest.owo.mixin.ui.access.BaseOwoHandledScreenAccessor;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.generators.BioGeneratorEntity;
import rearth.oritech.block.entity.generators.FuelGeneratorEntity;
import rearth.oritech.block.entity.generators.LavaGeneratorEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.block.entity.processing.*;
import rearth.oritech.client.ui.BasicMachineScreen;
import rearth.oritech.client.ui.PlayerModifierScreen;
import rearth.oritech.client.ui.ReactorScreen;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class OritechJeiPlugin implements IModPlugin {
    
    @Override
    public @NotNull Identifier getPluginUid() {
        return Oritech.id("jei_plugin");
    }
    
    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        
        registerOritechCategory(registration, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK, PulverizerBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK, FragmentForgeBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK, AssemblerBlockEntity.class);
        registerOritechCategory(registration, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK, FoundryBlockEntity.class);
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
          new OritechRecipeCategory(type, machineClass, block, registration.getJeiHelpers().getGuiHelper()));
    }
    
    private void registerCustom(IRecipeCategoryRegistration registration, OritechRecipeType type, Block block, Boolean isGenerator, List<ScreenProvider.GuiSlot> slots, InventorySlotAssignment assignments) {
        registration.addRecipeCategories(
          new OritechRecipeCategory(type, block, registration.getJeiHelpers().getGuiHelper(), isGenerator, slots, assignments));
    }
    
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        
        registerRecipe(registration, RecipeContent.PULVERIZER);
        registerRecipe(registration, RecipeContent.GRINDER);
        registerRecipe(registration, RecipeContent.ASSEMBLER);
        registerRecipe(registration, RecipeContent.FOUNDRY);
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
        var world = MinecraftClient.getInstance().world;
        var data = world.getRecipeManager().listAllOfType(type).stream().map(RecipeEntry::value).toList();
        registration.addRecipes(RecipeType.create(type.getIdentifier().getNamespace(), type.getIdentifier().getPath(), OritechRecipe.class), data);
    }
    
    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        
        registerCatalyst(registration, RecipeContent.PULVERIZER, BlockContent.PULVERIZER_BLOCK);
        registerCatalyst(registration, RecipeContent.GRINDER, BlockContent.FRAGMENT_FORGE_BLOCK);
        registerCatalyst(registration, RecipeContent.ASSEMBLER, BlockContent.ASSEMBLER_BLOCK);
        registerCatalyst(registration, RecipeContent.FOUNDRY, BlockContent.FOUNDRY_BLOCK);
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
        
    }
    
    private static class JeiExclusionZoneHandler implements IGuiContainerHandler<BaseOwoHandledScreen<FlowLayout, ?>> {
        @Override
        public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull BaseOwoHandledScreen<FlowLayout, ?> containerScreen) {
            return getScreenExclusionZones(containerScreen);
        }
    }
    
    private static @NotNull ArrayList<Rect2i> getScreenExclusionZones(@NotNull BaseOwoHandledScreen<FlowLayout, ?> containerScreen) {
        var result = new ArrayList<Rect2i>();
        
        // basically a copy of the owo emi adapter
        if (!containerScreen.children().isEmpty() && containerScreen instanceof BaseOwoHandledScreenAccessor accessor) {
            OwoUIAdapter<?> adapter = accessor.owo$getUIAdapter();
            if (adapter != null) {
                ParentComponent rootComponent = adapter.rootComponent;
                ArrayList<Component> children = new ArrayList<>();
                rootComponent.collectDescendants(children);
                children.remove(rootComponent);
                children.forEach((component) -> {
                    if (component instanceof ParentComponent parent) {
                        if (parent.surface() == Surface.BLANK) {
                            return;
                        }
                    }
                    
                    Size size = component.fullSize();
                    result.add(new Rect2i(component.x(), component.y(), size.width(), size.height()));
                });
            }
        }
        
        return result;
    }
}
