package rearth.oritech.init.datagen.data;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class TagContent {
    
    // only add tags for common stuff, such as dusts and nuggets. Clumps and gems are oritech-only
    // items
    // vanilla variants
    public static final TagKey<Item> COPPER_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/copper"));
    public static final TagKey<Item> COPPER_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/copper"));
    public static final TagKey<Item> IRON_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/iron"));
    public static final TagKey<Item> GOLD_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/gold"));
    public static final TagKey<Item> QUARTZ_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/quartz"));
    public static final TagKey<Item> COAL_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/coal"));
    
    public static final TagKey<Item> STEEL_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/steel"));
    
    // custom ores
    public static final TagKey<Item> NICKEL_RAW_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_ores/nickel"));
    public static final TagKey<Item> NICKEL_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/nickel"));
    public static final TagKey<Item> NICKEL_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/nickel"));
    public static final TagKey<Item> NICKEL_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/nickel"));
    public static final TagKey<Item> NICKEL_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/nickel"));
    
    public static final TagKey<Item> PLATINUM_RAW_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_ores/platinum"));
    public static final TagKey<Item> PLATINUM_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/platinum"));
    public static final TagKey<Item> PLATINUM_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/platinum"));
    public static final TagKey<Item> PLATINUM_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/platinum"));
    public static final TagKey<Item> PLATINUM_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/platinum"));
    
    // biomass
    public static final TagKey<Item> BIOMASS = TagKey.of(RegistryKeys.ITEM, Oritech.id("biomass"));
    
    // plating
    public static final TagKey<Item> MACHINE_PLATING = TagKey.of(RegistryKeys.ITEM, Oritech.id("plating"));

    // silicon
    public static final TagKey<Item> SILICON = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "silicon"));

    // carbon fibre
    public static final TagKey<Item> CARBON_FIBRE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "carbon_fibre"));

    // wires
    public static final TagKey<Item> WIRES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "wires"));
    
    // blocks
    public static final TagKey<Block> DRILL_MINEABLE = TagKey.of(RegistryKeys.BLOCK, Oritech.id("mineable/drill"));
    public static final TagKey<Block> RESOURCE_NODES = TagKey.of(RegistryKeys.BLOCK, Oritech.id("resource_nodes"));
    public static final TagKey<Block> LASER_PASSTHROUGH = TagKey.of(RegistryKeys.BLOCK, Oritech.id("laser_passthrough"));

    // trees
    public static final TagKey<Block> CUTTER_LOGS_MINEABLE = TagKey.of(RegistryKeys.BLOCK, Oritech.id("mineable/cutter_logs"));
    public static final TagKey<Block> CUTTER_LEAVES_MINEABLE = TagKey.of(RegistryKeys.BLOCK, Oritech.id("mineable/cutter_leaves"));

    // dyes
    public static final TagKey<Item> RAW_WHITE_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/white"));
    public static final TagKey<Item> RAW_LIGHT_GRAY_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/light_gray"));
    public static final TagKey<Item> RAW_BLACK_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/black"));
    public static final TagKey<Item> RAW_RED_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/red"));
    public static final TagKey<Item> RAW_ORANGE_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/orange"));
    public static final TagKey<Item> RAW_YELLOW_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/yellow"));
    public static final TagKey<Item> RAW_CYAN_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/cyan"));
    public static final TagKey<Item> RAW_BLUE_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/blue"));
    public static final TagKey<Item> RAW_MAGENTA_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/magenta"));
    public static final TagKey<Item> RAW_PINK_DYE = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/dyes/pink"));
}
