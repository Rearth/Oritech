package rearth.oritech.init.compat.emi.recipes;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;

public class OritechEmiRecipe implements EmiRecipe {
    private final Identifier id;
    private final List<EmiIngredient> itemInput;
    private final List<EmiStack> itemOutput;
    private final EmiIngredient fluidInput;
    private final EmiStack fluidOutput;
    private final List<EmiIngredient> totalInput;
    private final List<EmiStack> totalOutput;
    private final int time;

    private final EmiRecipeCategory category;
    private final MachineBlockEntity screenProvider;

    public OritechEmiRecipe(RecipeEntry<OritechRecipe> entry, EmiRecipeCategory category, Class<? extends MachineBlockEntity> screenProviderSource) {
        if (entry.value().getFluidInput() != null) {
            this.fluidInput = EmiStack.of(entry.value().getFluidInput().variant().getFluid(), entry.value().getFluidInput().amount());
        } else {
            this.fluidInput = null;
        }
        if (entry.value().getFluidOutput() != null) {
            this.fluidOutput = EmiStack.of(entry.value().getFluidOutput().variant().getFluid(), entry.value().getFluidOutput().amount());
        } else {
            this.fluidOutput = null;
        }

        this.id = entry.id();

        List<EmiIngredient> input = new ArrayList<>(entry.value().getInputs().stream().map(EmiIngredient::of).toList());
        List<EmiStack> output = new ArrayList<>(entry.value().getResults().stream().map(EmiStack::of).toList());

        this.itemInput = List.copyOf(input);
        this.itemOutput = List.copyOf(output);

        if (this.fluidInput != null) {
            input.add(this.fluidInput);
        }
        if (this.fluidOutput != null) {
            output.add(this.fluidOutput);
        }

        this.totalInput = List.copyOf(input);
        this.totalOutput = List.copyOf(output);

        this.time = entry.value().getTime();

        this.category = category;
        try {
            this.screenProvider = screenProviderSource.getDeclaredConstructor(BlockPos.class, BlockState.class).newInstance(new BlockPos(0, 0, 0), Blocks.AIR.getDefaultState());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return this.category;
    }

    @Override
    public @Nullable Identifier getId() {
        return this.id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return this.totalInput;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return this.totalOutput;
    }

    @Override
    public int getDisplayWidth() {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return 66;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var slots = screenProvider.getGuiSlots();
        var slotOffsets = screenProvider.getSlots();
        var offsetX = 23;
        var offsetY = 17;

        // inputs
        for (int i = 0; i < this.itemInput.size(); i++) {
            var entry = this.itemInput.get(i);
            var pos = slots.get(slotOffsets.inputStart() + i);
            widgets.addSlot(entry, pos.x() - offsetX, pos.y() - offsetY);
        }

        // arrow
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 80 - offsetX, 35 - offsetY);

        // outputs
        for (int i = 0; i < this.itemOutput.size(); i++) {
            var entry = this.itemOutput.get(i);
            var pos = slots.get(slotOffsets.outputStart() + i);
            widgets.addSlot(entry, pos.x() - offsetX, pos.y() - offsetY).recipeContext(this);
        }

        // data
        var duration = String.format("%.0f", this.time / 20f);
        widgets.addText(Text.of(duration + "s"), (int) (getDisplayWidth() * 0.3), (int) (getDisplayHeight() * 0.97), 0xFFFFFF, true);

        // fluids
        if (this.fluidInput != null) {
            //root.child(rearth.oritech.client.ui.BasicMachineScreen.createFluidRenderer(fluid, 81000, new ScreenProvider.BarConfiguration(4, 5, 16, 50)));

            var text = Text.literal(fluidInput.getAmount() * 1000 / FluidConstants.BUCKET + " mB " + fluidInput.getEmiStacks().get(0).getName().getString()).formatted(Formatting.DARK_AQUA);
            widgets.addTexture(GUI_COMPONENTS, 3, 4, 18, 52, 48, 0, 14, 50, 98, 96)
                    .tooltip(List.of(TooltipComponent.of(text.asOrderedText())));
        }

        if (this.fluidOutput != null) {
            //root.child(rearth.oritech.client.ui.BasicMachineScreen.createFluidRenderer(fluid, 81000, new ScreenProvider.BarConfiguration(123, 5, 16, 50)));

            var text = Text.literal(fluidOutput.getAmount() * 1000 / FluidConstants.BUCKET + " mB " + fluidOutput.getEmiStacks().get(0).getName().getString()).formatted(Formatting.DARK_AQUA);
            widgets.addTexture(GUI_COMPONENTS, 122, 4, 18, 52, 48, 0, 14, 50, 98, 96)
                    .tooltip(List.of(TooltipComponent.of(text.asOrderedText())));
        }
    }
}
