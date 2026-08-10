package rearth.oritech.compat.datagen.tag;

import static rearth.oritech.util.TagUtils.cItemTag;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class CompatTags {
    private CompatTags() {}

    public final class Items {
        private Items() {}

        public static final TagKey<Item> C_ENDER_DUST = cItemTag("dusts/ender_pearl");
    }
}
