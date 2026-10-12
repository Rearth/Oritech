package rearth.oritech.spaceage.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.client.SpaceAgeClientRecipes;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.recipe.VacuumRecipe;

import java.util.List;

/**
 * Discovered by JEI only; common initialization never references this class.
 */
@JeiPlugin
public final class SpaceAgeJeiPlugin implements IModPlugin {

    public static final IRecipeHolderType<VacuumRecipe> TYPE = IRecipeHolderType.create(OritechSpaceAge.id("vacuum_crafting"));
    private IJeiRuntime runtime;
    private List<RecipeHolder<VacuumRecipe>> shown = List.of();

    public SpaceAgeJeiPlugin() {

        NeoForge.EVENT_BUS.addListener((RecipesReceivedEvent event) -> {
            if (runtime == null) return;
            runtime.getRecipeManager().hideRecipes(TYPE, shown);
            shown = List.copyOf(SpaceAgeClientRecipes.vacuum());
            runtime.getRecipeManager().addRecipes(TYPE, shown);
            runtime.getRecipeManager().unhideRecipes(TYPE, shown);
        });
    }

    @Override
    public Identifier getPluginUid() {

        return OritechSpaceAge.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {

        registration.addRecipeCategories(new Category(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {

        shown = List.copyOf(SpaceAgeClientRecipes.vacuum());
        registration.addRecipes(TYPE, shown);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {

        registration.addCraftingStation(TYPE, SpaceAgeBlocks.VACUUM_CRAFTER.get());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {

        this.runtime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {

        runtime = null;
        shown = List.of();
    }

    private static final class Category extends AbstractRecipeCategory<RecipeHolder<VacuumRecipe>> {

        Category(IGuiHelper gui) {

            super(TYPE, Component.translatable("block.oritech_space_age.vacuum_crafter"), gui.createDrawableItemLike(SpaceAgeBlocks.VACUUM_CRAFTER), 170, 82);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<VacuumRecipe> holder, IFocusGroup focuses) {

            var recipe = holder.value();
            builder.addInputSlot(14, 20).addItemStacks(recipe.first().display()).setStandardSlotBackground();
            builder.addInputSlot(42, 20).addItemStacks(recipe.second().display()).setStandardSlotBackground();
            builder.addOutputSlot(132, 20).add(recipe.result()).setOutputSlotBackground();
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<VacuumRecipe> holder, IFocusGroup focuses) {

            var recipe = holder.value();
            builder.addAnimatedRecipeArrow(recipe.ticks()).setPosition(83, 20);
            builder.addText(Component.translatable("screen.oritech_space_age.vacuum.zero_g"), 170, 12).setPosition(0, 0);
            builder.addText(Component.literal(recipe.ticks() / 20 + " s · " + recipe.rfPerTick() + " RF/t"), 170, 12).setPosition(0, 49);
            builder.addText(Component.literal((long) recipe.ticks() * recipe.rfPerTick() + " RF total"), 170, 12).setPosition(0, 65);
        }
    }
}
