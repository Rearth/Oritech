package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.machines.processing.AssemblerBlockEntity;
import rearth.oritech.block.entity.machines.processing.AtomicForgeBlockEntity;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.block.entity.machines.processing.FoundryBlockEntity;
import rearth.oritech.block.entity.machines.processing.FragmentForgeBlockEntity;
import rearth.oritech.block.entity.machines.processing.PulverizerBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.compat.emi.recipes.OritechEmiRecipe;
import rearth.oritech.init.recipes.RecipeContent;

public class OritechEMIPlugin implements EmiPlugin {
    // TODO: make this more similar to OritechREIPlugin

    public static final EmiStack PULIVERIZER_WORKSTATION = EmiStack.of(BlockContent.PULVERIZER_BLOCK);
    public static final EmiStack GRINDER_WORKSTATION = EmiStack.of(BlockContent.FRAGMENT_FORGE_BLOCK);
    public static final EmiStack ASSEMBLER_WORKSTATION = EmiStack.of(BlockContent.ASSEMBLER_BLOCK );
    public static final EmiStack FOUNDRY_WORKSTATION = EmiStack.of(BlockContent.FOUNDRY_BLOCK);
    public static final EmiStack CENTRIFUGE_WORKSTATION = EmiStack.of(BlockContent.CENTRIFUGE_BLOCK);
    public static final EmiStack ATOMIC_FORGE_WORKSTATION = EmiStack.of(BlockContent.ATOMIC_FORGE_BLOCK);

    public static final EmiRecipeCategory PULVERIZER_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "pulverizer"), PULIVERIZER_WORKSTATION);
    public static final EmiRecipeCategory GRINDER_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "grinder"), GRINDER_WORKSTATION);
    public static final EmiRecipeCategory ASSEMBLER_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "assembler"), ASSEMBLER_WORKSTATION);
    public static final EmiRecipeCategory FOUNDRY_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "foundry"), FOUNDRY_WORKSTATION);
    public static final EmiRecipeCategory CENTRIFUGE_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "centrifuge"), CENTRIFUGE_WORKSTATION);
    public static final EmiRecipeCategory CENTRIFUGE_FLUID_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "centrifuge_fluid"), CENTRIFUGE_WORKSTATION);
    public static final EmiRecipeCategory ATOMIC_FORGE_CATEGORY = new LazyEmiRecipeCategory(new Identifier(Oritech.MOD_ID, "atomic_forge"), ATOMIC_FORGE_WORKSTATION);

    @Override
    public void register(EmiRegistry registry) {
        // categories
        registry.addCategory(PULVERIZER_CATEGORY);
        registry.addCategory(GRINDER_CATEGORY);
        registry.addCategory(ASSEMBLER_CATEGORY);
        registry.addCategory(FOUNDRY_CATEGORY);
        registry.addCategory(CENTRIFUGE_CATEGORY);
        registry.addCategory(CENTRIFUGE_FLUID_CATEGORY);
        registry.addCategory(ATOMIC_FORGE_CATEGORY);

        // workstations
        registry.addWorkstation(PULVERIZER_CATEGORY, PULIVERIZER_WORKSTATION);
        registry.addWorkstation(GRINDER_CATEGORY, GRINDER_WORKSTATION);
        registry.addWorkstation(ASSEMBLER_CATEGORY, ASSEMBLER_WORKSTATION);
        registry.addWorkstation(FOUNDRY_CATEGORY, FOUNDRY_WORKSTATION);
        registry.addWorkstation(CENTRIFUGE_CATEGORY, CENTRIFUGE_WORKSTATION);
        registry.addWorkstation(CENTRIFUGE_FLUID_CATEGORY, CENTRIFUGE_WORKSTATION);
        registry.addWorkstation(ATOMIC_FORGE_CATEGORY, ATOMIC_FORGE_WORKSTATION);

        // recipes
        RecipeManager manager = registry.getRecipeManager();
        manager.listAllOfType(RecipeContent.PULVERIZER)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, PULVERIZER_CATEGORY, PulverizerBlockEntity.class))
                .forEach(registry::addRecipe);
        manager.listAllOfType(RecipeContent.GRINDER)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, GRINDER_CATEGORY, FragmentForgeBlockEntity.class))
                .forEach(registry::addRecipe);
        manager.listAllOfType(RecipeContent.ASSEMBLER)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, ASSEMBLER_CATEGORY, AssemblerBlockEntity.class))
                .forEach(registry::addRecipe);
        manager.listAllOfType(RecipeContent.FOUNDRY)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, FOUNDRY_CATEGORY, FoundryBlockEntity.class))
                .forEach(registry::addRecipe);
        manager.listAllOfType(RecipeContent.CENTRIFUGE)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, CENTRIFUGE_CATEGORY, CentrifugeBlockEntity.class))
                .forEach(registry::addRecipe);
        manager.listAllOfType(RecipeContent.CENTRIFUGE_FLUID)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, CENTRIFUGE_FLUID_CATEGORY, CentrifugeBlockEntity.class))
                .forEach(registry::addRecipe);
        manager.listAllOfType(RecipeContent.ATOMIC_FORGE)
                .stream()
                .map(entry -> new OritechEmiRecipe(entry, ATOMIC_FORGE_CATEGORY, AtomicForgeBlockEntity.class))
                .forEach(registry::addRecipe);
    }
}
