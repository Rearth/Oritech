package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LazyEmiRecipeCategory extends EmiRecipeCategory {
    public LazyEmiRecipeCategory(Identifier id, EmiRenderable icon) {
        super(id, icon);
    }

    @Override
    public Text getName() {
        return Text.translatable("rei.process." + getId());
    }
}
