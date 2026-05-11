package rearth.oritech.item.tools.harvesting;

import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.Tiers;
import rearth.oritech.init.TagContent;

public final class ElectricToolMaterial {

    public static final ToolMaterial INSTANCE = new ToolMaterial(
      Tiers.NETHERITE.getIncorrectBlocksForDrops(),
      1000,
      9f,
      3.0f,
      22,
      TagContent.ELECTRIC_TOOL_REPAIR_ITEMS
    );

    private ElectricToolMaterial() {
    }
}
