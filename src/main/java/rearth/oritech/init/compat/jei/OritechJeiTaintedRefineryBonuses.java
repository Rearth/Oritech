package rearth.oritech.init.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;

import java.util.List;

final class OritechJeiTaintedRefineryBonuses
        extends AbstractRecipeCategory<OritechJeiTaintedRefineryBonuses.BonusInfo> {

    OritechJeiTaintedRefineryBonuses(IGuiHelper guiHelper) {
        super(
                OritechJeiRecipeTypes.TAINTED_REFINERY_BONUSES,
                Component.translatable("emi.category.oritech.tainted_refinery_bonuses"),
                guiHelper.createDrawableItemLike(BlockContent.TAINTED_REFINERY_BLOCK.get()),
                160,
                90
        );
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BonusInfo recipe, IFocusGroup focuses) {
        builder.addText(
                        Component.translatable("emi.title.oritech.tainted_bonus." + recipe.bonusType()),
                        156, 10
                )
                .setPosition(2, 2);

        var rows = (recipe.blocks().size() + 7) / 8;
        builder.addText(
                        Component.translatable("emi.description.oritech.tainted_bonus." + recipe.bonusType()),
                        156, 20
                )
                .setPosition(2, 16 + rows * 18);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BonusInfo recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.blocks().size(); i++) {
            var column = i % 8;
            var row = i / 8;
            builder.addInputSlot(2 + column * 18, 14 + row * 18)
                    .add(recipe.blocks().get(i))
                    .setStandardSlotBackground();
        }
    }

    @Override
    public Identifier getIdentifier(BonusInfo recipe) {
        return Oritech.id("tainted_refinery_bonuses/" + recipe.bonusType());
    }

    record BonusInfo(String bonusType, List<Block> blocks) {

        static BonusInfo fromTag(TagKey<Block> tag, String bonusType) {
            var level = Minecraft.getInstance().level;
            var blocks = level == null
                    ? List.<Block>of()
                    : level.registryAccess().lookupOrThrow(Registries.BLOCK).get(tag)
                    .map(holders -> holders.stream().map(holder -> holder.value()).toList())
                    .orElseGet(List::of);
            return new BonusInfo(bonusType, blocks);
        }
    }
}
