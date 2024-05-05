package rearth.oritech.init.compat.emi.recipes;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class OritechEmiRecipe implements EmiRecipe {
    private final Identifier id;
    private List<EmiIngredient> input;
    private List<EmiStack> output;

    private final EmiIngredient fluidInput;
    private final EmiStack fluidOutput;
    private final int time;

    private final EmiRecipeCategory category;
    
    private final MachineBlockEntity screenProvider;

    public OritechEmiRecipe(RecipeEntry<OritechRecipe> entry, EmiRecipeCategory category, Class<? extends MachineBlockEntity> screenProviderSource) {
        this.id = entry.id();
        this.input = entry.value().getInputs().stream().map(EmiIngredient::of).toList();
        this.output = entry.value().getResults().stream().map(EmiStack::of).toList();

        if (entry.value().getFluidInput() != null) {
            this.fluidInput = EmiStack.of(entry.value().getFluidInput().variant().getFluid(), entry.value().getFluidInput().amount());
            this.input.add(fluidInput);
        } else {
            this.fluidInput = null;
        }
        if (entry.value().getFluidOutput() != null) {
            this.fluidOutput = EmiStack.of(entry.value().getFluidOutput().variant().getFluid(), entry.value().getFluidOutput().amount());
            this.output.add(this.fluidOutput);
        } else {
            this.fluidOutput = null;
        }
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
        return this.input;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return this.output;
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
        for (int i = 0; i < input.size(); i++) {
            var entry = input.get(i);
            var pos = slots.get(slotOffsets.inputStart() + i);
            widgets.addSlot(entry, pos.x() - offsetX, pos.y() - offsetY);
        }

        // arrow
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 80 - offsetX, 35 - offsetY);

        // outputs
        for (int i = 0; i < output.size(); i++) {
            var entry = output.get(i);
            var pos = slots.get(slotOffsets.outputStart() + i);
            widgets.addSlot(entry, pos.x() - offsetX, pos.y() - offsetY).recipeContext(this);
        }

        // data
        var duration = String.format("%.0f", this.time / 20f);
        widgets.addText(Text.of(duration + "s"), (int) (getDisplayWidth() * 0.3), (int) (getDisplayHeight() * 0.97), 0xFFFFFF, true);

        // fluids
        if (this.fluidInput != null) {
            // ???
        }

        if (this.fluidOutput != null) {
            // ???
        }
    }
}
