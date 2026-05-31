package rearth.oritech.item.tools.harvesting;

import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ToolMaterial;
import rearth.oritech.init.TagContent;

public final class PromethiumToolMaterial {

    public static final ToolMaterial INSTANCE = new ToolMaterial(
            Tiers.NETHERITE.getIncorrectBlocksForDrops(),
            10000,
            24f,
            5.0f,
            28,
            TagContent.PROMETHEUM_TOOL_REPAIR_ITEMS
    );

    private PromethiumToolMaterial() {
    }
}
