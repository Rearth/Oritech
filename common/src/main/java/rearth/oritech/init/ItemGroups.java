package rearth.oritech.init;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ItemGroups implements ArchitecturyRegistryContainer<CreativeModeTab> {
    
    private static final Map<ItemContent.Groups, List<ItemStack>> registered = new HashMap<>();
    public static void add(ItemContent.Groups group, ItemLike item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(new ItemStack(item));
    }
    public static void add(ItemContent.Groups group, ItemStack item) {
        registered.computeIfAbsent(group, k -> new ArrayList<>()).add(item);
    }
    
    public static final CreativeModeTab MACHINE_GROUP = CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.machines"),
      () -> new ItemStack(BlockContent.FERTILIZER_BLOCK.asItem()));
    
    public static final CreativeModeTab COMPONENT_GROUP = CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.components"),
      () -> new ItemStack(ItemContent.SUPER_AI_CHIP.asItem()));
    
    public static final CreativeModeTab EQUIPMENT_GROUP = CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.equipment"),
      () -> new ItemStack(ToolsContent.CHAINSAW.asItem()));
    
    public static final CreativeModeTab DECORATIVE_GROUP = CreativeTabRegistry.create(
      Component.translatable("itemgroup.oritech.decorative"),
      () -> new ItemStack(BlockContent.RESOURCE_NODE_PLATINUM.asItem()));
    
    @Override
    public ResourceKey<Registry<CreativeModeTab>> getRegistryType() {
        return Registries.CREATIVE_MODE_TAB;
    }
    
    @Override
    public Class<CreativeModeTab> getTargetFieldType() {
        return CreativeModeTab.class;
    }
    
    @Override
    public void postProcessField(String namespace, CreativeModeTab value, String identifier, Field field, RegistrySupplier<CreativeModeTab> supplier) {
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
