package rearth.oritech.spaceage.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.init.BlockContent;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.cubesat.CubeSatItem;
import rearth.oritech.spaceage.block.robot_arm.ActionCardItem;
import rearth.oritech.spaceage.block.robot_arm.RobotArmItem;
import rearth.oritech.spaceage.item.MissionCardItem;
import rearth.oritech.util.RegistryReflectionUtil;

public final class SpaceAgeItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OritechSpaceAge.MOD_ID);

    public static final DeferredItem<Item> MISSION_CARD =
            ITEMS.registerItem("mission_card", MissionCardItem::new, () -> new Item.Properties().stacksTo(1));

    public static final DeferredItem<Item> ACTION_CARD = ITEMS.registerItem("action_card", ActionCardItem::new, () -> new Item.Properties().stacksTo(1));

    private SpaceAgeItems() {
    }

    @SuppressWarnings("unchecked")
    public static void addBlockItems() {

        RegistryReflectionUtil.IterateFields(SpaceAgeBlocks.class, DeferredBlock.class, (field, identifier, value) -> {
            if (field.isAnnotationPresent(BlockContent.NoBlockItem.class)) return;

            var block = (DeferredBlock<? extends Block>) value;
            var name = block.unwrapKey().orElseThrow().identifier().getPath();
            // Each cube-sat holds a whole rocket.
            if (block == SpaceAgeBlocks.CUBE_SAT) ITEMS.registerItem(name, properties -> new CubeSatItem(block.get(), properties),
                    () -> new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
            else if (block == SpaceAgeBlocks.ROBOT_ARM) ITEMS.registerItem(name, properties -> new RobotArmItem(block.get(), properties),
                    () -> new Item.Properties().useBlockDescriptionPrefix());
            else ITEMS.registerSimpleBlockItem(name, block);
        });
    }
}
