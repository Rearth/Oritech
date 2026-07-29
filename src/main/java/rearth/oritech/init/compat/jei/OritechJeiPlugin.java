package rearth.oritech.init.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.generators.BioGeneratorEntity;
import rearth.oritech.block.entity.generators.FuelGeneratorEntity;
import rearth.oritech.block.entity.generators.LavaGeneratorEntity;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.block.entity.processing.AssemblerBlockEntity;
import rearth.oritech.block.entity.processing.AtomicForgeBlockEntity;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.block.entity.processing.IndustrialChillerBlockEntity;
import rearth.oritech.block.entity.processing.FoundryBlockEntity;
import rearth.oritech.block.entity.processing.FragmentForgeBlockEntity;
import rearth.oritech.block.entity.processing.PulverizerBlockEntity;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.client.ui.ItemFilterScreen;
import rearth.oritech.client.ui.CentrifugeScreenHandler;
import rearth.oritech.client.ui.OritechMachineScreen;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.client.ui.CyberneticAugmentationCenterScreen;
import rearth.oritech.client.ui.RefineryScreenHandler;
import rearth.oritech.client.ui.ReactorScreen;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.function.Supplier;

@JeiPlugin
public final class OritechJeiPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Oritech.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();

        registerMachineCategory(registration, OritechJeiRecipeTypes.PULVERIZER, BlockContent.PULVERIZER.get(),
                PulverizerBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.GRINDER, BlockContent.FRAGMENT_FORGE.get(),
                FragmentForgeBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.ASSEMBLER, BlockContent.ASSEMBLER.get(),
                AssemblerBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.FOUNDRY, BlockContent.FOUNDRY.get(),
                FoundryBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.REFINERY, BlockContent.REFINERY.get(),
                RefineryBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.INDUSTRIAL_CHILLER, BlockContent.INDUSTRIAL_CHILLER.get(),
                IndustrialChillerBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.CENTRIFUGE, BlockContent.CENTRIFUGE.get(),
                CentrifugeBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE.get(),
                CentrifugeBlockEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE.get(),
                AtomicForgeBlockEntity.class);

        registerMachineCategory(registration, OritechJeiRecipeTypes.BIO_GENERATOR, BlockContent.BIO_GENERATOR.get(),
                BioGeneratorEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.FUEL_GENERATOR, BlockContent.FUEL_GENERATOR.get(),
                FuelGeneratorEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.LAVA_GENERATOR, BlockContent.LAVA_GENERATOR.get(),
                LavaGeneratorEntity.class);
        registerMachineCategory(registration, OritechJeiRecipeTypes.STEAM_ENGINE, BlockContent.STEAM_ENGINE.get(),
                SteamEngineEntity.class);

        registration.addRecipeCategories(new OritechJeiRecipeCategory(
                OritechJeiRecipeTypes.REACTOR,
                BlockContent.NUCLEAR_REACTOR_CONTROLLER.get(),
                guiHelper,
                List.of(new ScreenProvider.GuiSlot(0, 55, 35)),
                new ContainerSlotAssignment(0, 1, 1, 0)
        ));
        registration.addRecipeCategories(new OritechJeiParticleCollisionRecipe(guiHelper));
        registration.addRecipeCategories(new OritechJeiLaserRecipe(guiHelper));
        registration.addRecipeCategories(new OritechJeiTaintedRefineryCreation(guiHelper));
        registration.addRecipeCategories(new OritechJeiTaintedRefineryBonuses(guiHelper));
    }

    private static void registerMachineCategory(IRecipeCategoryRegistration registration,
                                                IRecipeHolderType<OritechRecipe> recipeType,
                                                Block machine,
                                                Class<? extends MachineBlockEntity> machineClass) {
        registration.addRecipeCategories(new OritechJeiRecipeCategory(
                recipeType,
                machineClass,
                machine,
                registration.getJeiHelpers().getGuiHelper()
        ));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerRecipes(registration, OritechJeiRecipeTypes.PULVERIZER, RecipeContent.PULVERIZER);
        registerRecipes(registration, OritechJeiRecipeTypes.GRINDER, RecipeContent.GRINDER);
        registerRecipes(registration, OritechJeiRecipeTypes.ASSEMBLER, RecipeContent.ASSEMBLER);
        registerRecipes(registration, OritechJeiRecipeTypes.FOUNDRY, RecipeContent.FOUNDRY);
        registerRecipes(registration, OritechJeiRecipeTypes.REFINERY, RecipeContent.REFINERY);
        registerRecipes(registration, OritechJeiRecipeTypes.INDUSTRIAL_CHILLER, RecipeContent.INDUSTRIAL_CHILLER);
        registerRecipes(registration, OritechJeiRecipeTypes.CENTRIFUGE, RecipeContent.CENTRIFUGE);
        registerRecipes(registration, OritechJeiRecipeTypes.CENTRIFUGE_FLUID, RecipeContent.CENTRIFUGE_FLUID);
        registerRecipes(registration, OritechJeiRecipeTypes.ATOMIC_FORGE, RecipeContent.ATOMIC_FORGE);

        registerRecipes(registration, OritechJeiRecipeTypes.BIO_GENERATOR, RecipeContent.BIO_GENERATOR);
        registerRecipes(registration, OritechJeiRecipeTypes.FUEL_GENERATOR, RecipeContent.FUEL_GENERATOR);
        registerRecipes(registration, OritechJeiRecipeTypes.LAVA_GENERATOR, RecipeContent.LAVA_GENERATOR);
        registerRecipes(registration, OritechJeiRecipeTypes.STEAM_ENGINE, RecipeContent.STEAM_ENGINE);

        registerRecipes(registration, OritechJeiRecipeTypes.PARTICLE_COLLISION, RecipeContent.PARTICLE_COLLISION);
        registerRecipes(registration, OritechJeiRecipeTypes.LASER, RecipeContent.LASER);
        registerRecipes(registration, OritechJeiRecipeTypes.REACTOR, RecipeContent.REACTOR);

        registration.addRecipes(OritechJeiRecipeTypes.TAINTED_REFINERY_CREATION,
                List.of(new OritechJeiTaintedRefineryCreation.CreationInfo()));
        registration.addRecipes(OritechJeiRecipeTypes.TAINTED_REFINERY_BONUSES, List.of(
                OritechJeiTaintedRefineryBonuses.BonusInfo.fromTag(
                        TagContent.REFINERY_SCULK_BLOCKS, "sculk"),
                OritechJeiTaintedRefineryBonuses.BonusInfo.fromTag(
                        TagContent.REFINERY_ARCANE_BLOCKS, "arcane")
        ));
    }

    private static void registerRecipes(IRecipeRegistration registration,
                                        IRecipeHolderType<OritechRecipe> jeiRecipeType,
                                        Supplier<RecipeType<OritechRecipe>> recipeType) {
        registration.addRecipes(
                jeiRecipeType,
                List.copyOf(OritechJeiRecipeSync.getRecipes(recipeType.get()))
        );
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                UpgradableOritechScreenHandler.class, ModScreens.PULVERIZER_SCREEN.get(),
                OritechJeiRecipeTypes.PULVERIZER, 0, 1, 3, 36);
        registration.addRecipeTransferHandler(
                UpgradableOritechScreenHandler.class, ModScreens.GRINDER_SCREEN.get(),
                OritechJeiRecipeTypes.GRINDER, 0, 1, 4, 36);
        registration.addRecipeTransferHandler(
                UpgradableOritechScreenHandler.class, ModScreens.ASSEMBLER_SCREEN.get(),
                OritechJeiRecipeTypes.ASSEMBLER, 0, 4, 5, 36);
        registration.addRecipeTransferHandler(
                UpgradableOritechScreenHandler.class, ModScreens.FOUNDRY_SCREEN.get(),
                OritechJeiRecipeTypes.FOUNDRY, 0, 2, 3, 36);
        registration.addRecipeTransferHandler(
                OritechScreenHandler.class, ModScreens.ATOMIC_FORGE_SCREEN.get(),
                OritechJeiRecipeTypes.ATOMIC_FORGE, 0, 3, 4, 36);
        registration.addRecipeTransferHandler(
                RefineryScreenHandler.class, ModScreens.REFINERY_SCREEN.get(),
                OritechJeiRecipeTypes.REFINERY, 0, 1, 2, 36);
        registration.addRecipeTransferHandler(
                CentrifugeScreenHandler.class, ModScreens.CENTRIFUGE_SCREEN.get(),
                OritechJeiRecipeTypes.CENTRIFUGE, 0, 1, 3, 36);
        registration.addRecipeTransferHandler(
                CentrifugeScreenHandler.class, ModScreens.CENTRIFUGE_SCREEN.get(),
                OritechJeiRecipeTypes.CENTRIFUGE_FLUID, 0, 1, 3, 36);
        registration.addRecipeTransferHandler(
                UpgradableOritechScreenHandler.class, ModScreens.POWERED_FURNACE_SCREEN.get(),
                RecipeTypes.SMELTING, 0, 1, 2, 36);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registerCraftingStations(registration, OritechJeiRecipeTypes.PULVERIZER, BlockContent.PULVERIZER.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.GRINDER, BlockContent.FRAGMENT_FORGE.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.ASSEMBLER, BlockContent.ASSEMBLER.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.FOUNDRY, BlockContent.FOUNDRY.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.REFINERY,
                BlockContent.REFINERY.get(), BlockContent.TAINTED_REFINERY.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.INDUSTRIAL_CHILLER, BlockContent.INDUSTRIAL_CHILLER.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.CENTRIFUGE, BlockContent.CENTRIFUGE.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.CENTRIFUGE_FLUID, BlockContent.CENTRIFUGE.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.ATOMIC_FORGE, BlockContent.ATOMIC_FORGE.get());

        registerCraftingStations(registration, OritechJeiRecipeTypes.BIO_GENERATOR, BlockContent.BIO_GENERATOR.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.FUEL_GENERATOR, BlockContent.FUEL_GENERATOR.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.LAVA_GENERATOR, BlockContent.LAVA_GENERATOR.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.STEAM_ENGINE, BlockContent.STEAM_ENGINE.get());

        registerCraftingStations(registration, OritechJeiRecipeTypes.PARTICLE_COLLISION,
                BlockContent.PARTICLE_ACCELERATOR.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.LASER, BlockContent.ENDERIC_LASER.get());
        registerCraftingStations(registration, OritechJeiRecipeTypes.REACTOR, BlockContent.NUCLEAR_REACTOR_CONTROLLER.get());

        registration.addCraftingStation(OritechJeiRecipeTypes.TAINTED_REFINERY_CREATION,
                BlockContent.REFINERY.get(), BlockContent.ARCANE_CATALYST.get());
        registration.addCraftingStation(OritechJeiRecipeTypes.TAINTED_REFINERY_BONUSES,
                BlockContent.TAINTED_REFINERY.get());

        registration.addCraftingStation(RecipeTypes.SMELTING, BlockContent.POWERED_FURNACE.get());
    }

    private static void registerCraftingStations(IRecipeCatalystRegistration registration,
                                                 IRecipeHolderType<OritechRecipe> recipeType,
                                                 Block... machines) {
        registration.addCraftingStation(recipeType, machines);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        var exclusionHandler = new JeiExclusionZoneHandler();
        registration.addGenericGuiContainerHandler(ReactorScreen.class, exclusionHandler);
        registration.addGenericGuiContainerHandler(CyberneticAugmentationCenterScreen.class, exclusionHandler);
        registration.addGenericGuiContainerHandler(OritechMachineScreen.class, exclusionHandler);
        registration.addGhostIngredientHandler(ItemFilterScreen.class, new JeiItemFilterGhostHandler());
    }
}
