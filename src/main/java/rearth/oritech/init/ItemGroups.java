package rearth.oritech.init;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

public class ItemGroups {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface ItemGroupTarget {
        ItemGroups.GROUPS value();
    }

    public enum GROUPS {
        first, second
    }

    private static final Map<GROUPS, List<ItemConvertible>> registered = new HashMap<>();
    public static void add(GROUPS group, ItemConvertible item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(item);
    }

    private static final ItemGroup ORITECH_GROUP = FabricItemGroup.builder().entries(((context, entries) -> {
        for (var item : registered.get(GROUPS.first) ) {
            entries.add(item);
        }
    })).displayName(Text.literal("Oritech")).icon(() -> new ItemStack(ItemContent.BANANA)).build();

    private static final ItemGroup ORITECH_SECOND_GROUP = FabricItemGroup.builder().entries(((context, entries) -> {
        for (var item : registered.get(GROUPS.second) ) {
            entries.add(item);
        }
    })).displayName(Text.literal("More Oritech")).icon(() -> new ItemStack(ItemContent.BANANA)).build();

    public static void registerItemGroup() {

        Oritech.LOGGER.info("Registering Oritech Items");

        Registry.register(Registries.ITEM_GROUP, new Identifier(Oritech.MOD_ID, "main"), ORITECH_GROUP);
        Registry.register(Registries.ITEM_GROUP, new Identifier(Oritech.MOD_ID, "second"), ORITECH_SECOND_GROUP);
    }

}
