package rearth.oritech.init;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemGroups implements ArchitecturyRegistryContainer<ItemGroup> {
    
    private static final Map<ItemContent.Groups, List<ItemStack>> registered = new HashMap<>();
    public static void add(ItemContent.Groups group, ItemConvertible item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(new ItemStack(item));
    }
    public static void add(ItemContent.Groups group, ItemStack item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(item);
    }
    
    public static final ItemGroup MACHINE_GROUP = CreativeTabRegistry.create(
      Text.translatable("itemgroup.oritech.machines"),
      () -> new ItemStack(BlockContent.FERTILIZER_BLOCK.asItem()));
    
    public static final ItemGroup COMPONENT_GROUP = CreativeTabRegistry.create(
      Text.translatable("itemgroup.oritech.components"),
      () -> new ItemStack(ItemContent.SUPER_AI_CHIP.asItem()));
    
    public static final ItemGroup EQUIPMENT_GROUP = CreativeTabRegistry.create(
      Text.translatable("itemgroup.oritech.equipment"),
      () -> new ItemStack(ToolsContent.CHAINSAW.asItem()));
    
    public static final ItemGroup DECORATIVE_GROUP = CreativeTabRegistry.create(
      Text.translatable("itemgroup.oritech.decorative"),
      () -> new ItemStack(BlockContent.RESOURCE_NODE_PLATINUM.asItem()));
    
    @Override
    public RegistryKey<Registry<ItemGroup>> getRegistryType() {
        return RegistryKeys.ITEM_GROUP;
    }
    
    @Override
    public Class<ItemGroup> getTargetFieldType() {
        return ItemGroup.class;
    }
    
    @Override
    public void postProcessField(String namespace, ItemGroup value, String identifier, Field field, RegistrySupplier<ItemGroup> supplier) {
        ArchitecturyRegistryContainer.super.postProcessField(namespace, value, identifier, field, supplier);
        
        List<ItemStack> items = null;
        if (value.equals(MACHINE_GROUP)) items = registered.get(ItemContent.Groups.machines);
        if (value.equals(COMPONENT_GROUP)) items = registered.get(ItemContent.Groups.components);
        if (value.equals(EQUIPMENT_GROUP)) items = registered.get(ItemContent.Groups.equipment);
        if (value.equals(DECORATIVE_GROUP)) items = registered.get(ItemContent.Groups.decorative);
        
        if (items == null) {
            System.err.println("unknown item group: " + identifier);
            return;
        }
        
        items.forEach(item -> CreativeTabRegistry.appendStack(supplier, item));
        
    }
}
