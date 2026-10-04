package rearth.oritech.spaceage.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.RegistryReflectionUtil;
import rearth.oritech.spaceage.OritechSpaceAge;

public final class SpaceAgeItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OritechSpaceAge.MOD_ID);

    public static final net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> MISSION_CARD =
            ITEMS.registerItem("mission_card", rearth.oritech.spaceage.item.MissionCardItem::new, () -> new net.minecraft.world.item.Item.Properties().stacksTo(1));

    private SpaceAgeItems() {
    }

    @SuppressWarnings("unchecked")
    public static void addBlockItems() {
        RegistryReflectionUtil.IterateFields(SpaceAgeBlocks.class, DeferredBlock.class, (field, identifier, value) -> {
            if (field.isAnnotationPresent(BlockContent.NoBlockItem.class)) return;

            var block = (DeferredBlock<? extends Block>) value;
            var name = block.unwrapKey().orElseThrow().identifier().getPath();
            ITEMS.registerSimpleBlockItem(name, block);
        });
    }
}
