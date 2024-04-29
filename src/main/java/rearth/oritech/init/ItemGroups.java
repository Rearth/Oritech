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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemGroups {
    
    private static final Map<ItemContent.Groups, List<ItemConvertible>> registered = new HashMap<>();
    public static void add(ItemContent.Groups group, ItemConvertible item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(item);
    }

    private static final ItemGroup MACHINE_GROUP = FabricItemGroup.builder().entries(((context, entries) -> {
        for (var item : registered.get(ItemContent.Groups.machines) ) {
            entries.add(item);
        }
    })).displayName(Text.translatable("itemgroup.oritech.machines")).icon(() -> new ItemStack(BlockContent.FERTILIZER_BLOCK.asItem())).build();

    private static final ItemGroup COMPONENT_GROUP = FabricItemGroup.builder().entries(((context, entries) -> {
        for (var item : registered.get(ItemContent.Groups.components) ) {
            entries.add(item);
        }
    })).displayName(Text.translatable("itemgroup.oritech.components")).icon(() -> new ItemStack(ItemContent.SUPER_AI_CHIP)).build();

    private static final ItemGroup EQUIPMENT_GROUP = FabricItemGroup.builder().entries(((context, entries) -> {
        for (var item : registered.get(ItemContent.Groups.equipment) ) {
            entries.add(item);
        }
    })).displayName(Text.translatable("itemgroup.oritech.equipment")).icon(() -> new ItemStack(ToolsContent.CHAINSAW)).build();

    private static final ItemGroup DECORATIVE_GROUP = FabricItemGroup.builder().entries(((context, entries) -> {
        for (var item : registered.get(ItemContent.Groups.decorative) ) {
            entries.add(item);
        }
    })).displayName(Text.translatable("itemgroup.oritech.decorative")).icon(() -> new ItemStack(BlockContent.RESOURCE_NODE_PLATINUM)).build();

    public static void registerItemGroup() {

        Oritech.LOGGER.info("Registering Oritech Items");

        Registry.register(Registries.ITEM_GROUP, new Identifier(Oritech.MOD_ID, "machines"), MACHINE_GROUP);
        Registry.register(Registries.ITEM_GROUP, new Identifier(Oritech.MOD_ID, "components"), COMPONENT_GROUP);
        Registry.register(Registries.ITEM_GROUP, new Identifier(Oritech.MOD_ID, "equipment"), EQUIPMENT_GROUP);
        Registry.register(Registries.ITEM_GROUP, new Identifier(Oritech.MOD_ID, "decorative"), DECORATIVE_GROUP);
    }

}
