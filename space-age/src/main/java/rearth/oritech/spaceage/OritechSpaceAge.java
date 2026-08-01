package rearth.oritech.spaceage;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import rearth.oritech.Oritech;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeCreativeTabs;
import rearth.oritech.spaceage.init.SpaceAgeItems;

@Mod(OritechSpaceAge.MOD_ID)
public final class OritechSpaceAge {

    public static final String MOD_ID = "oritech_space_age";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OritechSpaceAge(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Oritech: Space Age with {}", Oritech.MOD_ID);

        SpaceAgeBlocks.BLOCKS.register(modEventBus);
        SpaceAgeItems.ITEMS.register(modEventBus);
        SpaceAgeBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        SpaceAgeCreativeTabs.TABS.register(modEventBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
