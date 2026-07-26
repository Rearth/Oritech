package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.ui.OritechMachineScreen;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

final class OritechJeiRecipeCategory extends AbstractRecipeCategory<RecipeHolder<OritechRecipe>> {

    private static final int WIDTH = 150;
    private static final int HEIGHT = 70;
    private static final int GUI_SLOT_OFFSET_X = 23;
    private static final int GUI_SLOT_OFFSET_Y = 17;

    private final Block machine;
    private final List<ScreenProvider.GuiSlot> slots;
    private final ContainerSlotAssignment slotAssignments;
    private final ScreenProvider.ArrowConfiguration indicator;
    private final IDrawableStatic fluidBackground;

    OritechJeiRecipeCategory(IRecipeHolderType<OritechRecipe> recipeType,
                             Class<? extends MachineBlockEntity> machineClass,
                             Block machine,
                             IGuiHelper guiHelper) {
        this(recipeType, machine, guiHelper, createScreenProvider(machineClass, machine));
    }

    OritechJeiRecipeCategory(IRecipeHolderType<OritechRecipe> recipeType,
                             Block machine,
                             IGuiHelper guiHelper,
                             List<ScreenProvider.GuiSlot> slots,
                             ContainerSlotAssignment slotAssignments) {
        this(recipeType, machine, guiHelper, slots, slotAssignments, new ScreenProvider.ArrowConfiguration(
                rearth.oritech.Oritech.id("textures/gui/modular/arrow_empty.png"),
                rearth.oritech.Oritech.id("textures/gui/modular/arrow_full.png"),
                80, 35, 29, 16, true
        ));
    }

    private OritechJeiRecipeCategory(IRecipeHolderType<OritechRecipe> recipeType,
                                     Block machine,
                                     IGuiHelper guiHelper,
                                     MachineBlockEntity screenProvider) {
        this(recipeType, machine, guiHelper, screenProvider.getGuiSlots(),
                screenProvider.getSlotAssignments(),
                screenProvider.getIndicatorConfiguration());
    }

    private OritechJeiRecipeCategory(IRecipeHolderType<OritechRecipe> recipeType,
                                     Block machine,
                                     IGuiHelper guiHelper,
                                     List<ScreenProvider.GuiSlot> slots,
                                     ContainerSlotAssignment slotAssignments,
                                     ScreenProvider.ArrowConfiguration indicator) {
        super(
                recipeType,
                Component.translatable("emi.category.oritech." + recipeType.getUid().getPath()),
                guiHelper.createDrawableItemLike(machine),
                WIDTH,
                HEIGHT
        );
        this.machine = machine;
        this.slots = slots;
        this.slotAssignments = slotAssignments;
        this.indicator = indicator;
        this.fluidBackground = guiHelper.drawableBuilder(OritechMachineScreen.GUI_COMPONENTS, 48, 0, 14, 50)
                .setTextureSize(98, 96)
                .build();
    }

    private static MachineBlockEntity createScreenProvider(Class<? extends MachineBlockEntity> machineClass, Block machine) {
        try {
            return machineClass.getDeclaredConstructor(BlockPos.class, BlockState.class)
                    .newInstance(BlockPos.ZERO, machine.defaultBlockState());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to create JEI layout source for " + machineClass.getName(), exception);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<OritechRecipe> holder,
                                   IFocusGroup focuses) {
        var recipe = holder.value();
        builder.addAnimatedRecipeArrow(40)
                .setPosition(indicator.x() - GUI_SLOT_OFFSET_X, indicator.y() - GUI_SLOT_OFFSET_Y);

        var seconds = String.format("%.0f", recipe.time() / 20f);
        var textX = (int) (WIDTH * 0.35);
        builder.addText(
                        Component.translatable("emi.title.oritech.cookingtime", seconds, recipe.time()),
                        WIDTH - textX, 10
                )
                .setPosition(textX, (int) (HEIGHT * 0.9));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<OritechRecipe> holder, IFocusGroup focuses) {
        var recipe = holder.value();

        for (int i = 0; i < recipe.itemInputs().size(); i++) {
            var input = recipe.itemInputs().get(i);
            if (input.isEmpty()) {
                continue;
            }

            var position = slots.get(slotAssignments.inputStart() + i);
            var y = Math.clamp(2, position.y() - GUI_SLOT_OFFSET_Y, HEIGHT - 22);
            builder.addInputSlot(position.x() - GUI_SLOT_OFFSET_X, y)
                    .add(input)
                    .setStandardSlotBackground();
        }

        recipe.fluidInput().ifPresent(fluidInput -> {
            var amount = fluidInput.amount();
            var slot = builder.addInputSlot(10, 6)
                    .setBackground(fluidBackground, -2, -2)
                    .setFluidRenderer(amount, false, 10, 46);
            fluidInput.ingredient().fluids().forEach(fluid -> slot.add(fluid.value(), amount));
        });

        var hasMultipleItemOutputs = recipe.itemResults().size() > 1;
        for (int i = 0; i < recipe.itemResults().size(); i++) {
            var position = slots.get(slotAssignments.outputStart() + i);
            var y = Math.clamp(1, position.y() - GUI_SLOT_OFFSET_Y, HEIGHT - 22);
            var outputSlot = builder.addOutputSlot(position.x() - GUI_SLOT_OFFSET_X, y)
                    .add(recipe.itemResults().get(i));
            if (hasMultipleItemOutputs) {
                outputSlot.setStandardSlotBackground();
            } else {
                outputSlot.setOutputSlotBackground();
            }
        }

        var tankX = recipe.fluidOutputs().size() > 1 ? 80 : 120;
        for (int i = 0; i < recipe.fluidOutputs().size(); i++) {
            var output = recipe.fluidOutputs().get(i);
            builder.addOutputSlot(tankX + i * 20, 6)
                    .add(output.fluid().value(), output.amount(), output.components())
                    .setBackground(fluidBackground, -2, -2)
                    .setFluidRenderer(output.amount(), false, 10, 46);
        }

        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION).add(machine);
    }
}
