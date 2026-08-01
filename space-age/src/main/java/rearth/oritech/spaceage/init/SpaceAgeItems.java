package rearth.oritech.spaceage.init;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;

public final class SpaceAgeItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OritechSpaceAge.MOD_ID);

    public static final DeferredItem<BlockItem> ROCKET_ASSEMBLER = ITEMS.registerSimpleBlockItem(SpaceAgeBlocks.ROCKET_ASSEMBLER);
    public static final DeferredItem<BlockItem> ROCKET_PAD = ITEMS.registerSimpleBlockItem(SpaceAgeBlocks.ROCKET_PAD);
    public static final DeferredItem<BlockItem> ROCKET_ENGINE_TIER_1 = ITEMS.registerSimpleBlockItem(SpaceAgeBlocks.ROCKET_ENGINE_TIER_1);
    public static final DeferredItem<BlockItem> ROCKET_ENGINE_TIER_2 = ITEMS.registerSimpleBlockItem(SpaceAgeBlocks.ROCKET_ENGINE_TIER_2);
    public static final DeferredItem<BlockItem> ROCKET_ENGINE_TIER_3 = ITEMS.registerSimpleBlockItem(SpaceAgeBlocks.ROCKET_ENGINE_TIER_3);

    private SpaceAgeItems() {
    }
}
