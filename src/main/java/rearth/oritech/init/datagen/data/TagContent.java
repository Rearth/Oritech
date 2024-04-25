package rearth.oritech.init.datagen.data;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class TagContent {
    
    // only add tags for common stuff, such as dusts and nuggets. Clumps and gems are oritech-only
    // items
    // vanilla variants
    public static final TagKey<Item> COPPER_DUSTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "copper_dusts"));
    public static final TagKey<Item> COPPER_NUGGETS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "copper_nuggets"));
    public static final TagKey<Item> IRON_DUSTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "iron_dusts"));
    public static final TagKey<Item> GOLD_DUSTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "gold_dusts"));
    
    public static final TagKey<Item> STEEL_INGOTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "steel_ingots"));
    
    // custom ores
    public static final TagKey<Item> NICKEL_RAW_ORES = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "raw_nickel_ores"));
    public static final TagKey<Item> NICKEL_DUSTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "nickel_dusts"));
    public static final TagKey<Item> NICKEL_NUGGETS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "nickel_nuggets"));
    public static final TagKey<Item> NICKEL_INGOTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "nickel_ingots"));
    public static final TagKey<Item> NICKEL_ORES = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "nickel_ores"));
    
    public static final TagKey<Item> PLATINUM_RAW_ORES = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "raw_platinum_ores"));
    public static final TagKey<Item> PLATINUM_DUSTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "platinum_dusts"));
    public static final TagKey<Item> PLATINUM_NUGGETS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "platinum_nuggets"));
    public static final TagKey<Item> PLATINUM_INGOTS = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "platinum_ingots"));
    public static final TagKey<Item> PLATINUM_ORES = TagKey.of(RegistryKeys.ITEM, new Identifier("c", "platinum_ores"));
    
    // blocks
    
}
