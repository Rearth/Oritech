package rearth.oritech.init;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import rearth.oritech.util.registry.OritechDeferredRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemGroups {
    
  public static final OritechDeferredRegistry<CreativeModeTab> TABS = OritechDeferredRegistry.create(Registries.CREATIVE_MODE_TAB);
    
    private static final Map<ItemContent.Groups, List<ItemStack>> registered = new HashMap<>();
    public static void add(ItemContent.Groups group, ItemLike item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(new ItemStack(item));
    }
    public static void add(ItemContent.Groups group, ItemStack item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(item);
    }
    
    public static final RegistrySupplier<CreativeModeTab> MACHINE_GROUP = TABS.register("machine_group", () -> CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.machines"),
      () -> new ItemStack(BlockContent.FERTILIZER_BLOCK.get().asItem())));
    
    public static final RegistrySupplier<CreativeModeTab> COMPONENT_GROUP = TABS.register("component_group", () -> CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.components"),
      () -> new ItemStack(ItemContent.SUPER_AI_CHIP.get().asItem())));
    
    public static final RegistrySupplier<CreativeModeTab> EQUIPMENT_GROUP = TABS.register("equipment_group", () -> CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.equipment"),
      () -> new ItemStack(ToolsContent.CHAINSAW.get().asItem())));
    
    public static final RegistrySupplier<CreativeModeTab> DECORATIVE_GROUP = TABS.register("decorative_group", () -> CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.decorative"),
      () -> new ItemStack(BlockContent.RESOURCE_NODE_PLATINUM.get().asItem())));
    
    public static void register() {
        TABS.register();
        append(MACHINE_GROUP, ItemContent.Groups.machines);
        append(COMPONENT_GROUP, ItemContent.Groups.components);
        append(EQUIPMENT_GROUP, ItemContent.Groups.equipment);
        append(DECORATIVE_GROUP, ItemContent.Groups.decorative);
    }

    private static void append(RegistrySupplier<CreativeModeTab> supplier, ItemContent.Groups group) {
        List<ItemStack> items = registered.get(group);
        
        if (items == null) {
            return;
        }
        
        items.forEach(item -> CreativeTabRegistry.appendStack(supplier, item));
    }
}
