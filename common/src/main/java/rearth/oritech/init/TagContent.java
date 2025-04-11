package rearth.oritech.init;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class TagContent {
    
    // only add tags for common stuff, such as dusts and nuggets. Gems are oritech-only
    // items
    // vanilla variants
    public static final TagKey<Item> CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps"));
    public static final TagKey<Item> DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts"));
    public static final TagKey<Item> COPPER_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/copper"));
    public static final TagKey<Item> COPPER_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/copper"));
    public static final TagKey<Item> COPPER_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/copper"));
    public static final TagKey<Item> COPPER_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/copper"));
    
    public static final TagKey<Item> IRON_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/iron"));
    public static final TagKey<Item> IRON_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/iron"));
    public static final TagKey<Item> IRON_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/iron"));

    public static final TagKey<Item> GOLD_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/gold"));
    public static final TagKey<Item> GOLD_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/gold"));
    public static final TagKey<Item> GOLD_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/gold"));
    public static final TagKey<Item> QUARTZ_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/quartz"));
    public static final TagKey<Item> COAL_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/coal"));
    public static final TagKey<Item> URANIUM_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/uranium"));
    public static final TagKey<Item> PLUTONIUM_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/plutonium"));
    public static final TagKey<Item> ELECTRUM_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/electrum"));
    
    public static final TagKey<Item> STEEL_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/steel"));
    public static final TagKey<Item> ELECTRUM_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/electrum"));
    
    // custom ores
    public static final TagKey<Item> NICKEL_RAW_MATERIALS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/nickel"));
    public static final TagKey<Item> NICKEL_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/nickel"));
    public static final TagKey<Item> NICKEL_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/nickel"));
    public static final TagKey<Item> NICKEL_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/nickel"));
    public static final TagKey<Item> NICKEL_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/nickel"));
    public static final TagKey<Item> NICKEL_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/nickel"));
    public static final TagKey<Block> NICKEL_ORE_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/nickel"));
    
    public static final TagKey<Item> PLATINUM_RAW_MATERIALS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/platinum"));
    public static final TagKey<Item> PLATINUM_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/platinum"));
    public static final TagKey<Item> PLATINUM_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/platinum"));
    public static final TagKey<Item> PLATINUM_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/platinum"));
    public static final TagKey<Item> PLATINUM_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/platinum"));
    public static final TagKey<Item> PLATINUM_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/platinum"));
    public static final TagKey<Block> PLATINUM_ORE_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/platinum"));
    
    public static final TagKey<Item> URANIUM_RAW_MATERIALS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/uranium"));
    public static final TagKey<Item> URANIUM_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/uranium"));
    public static final TagKey<Item> URANIUM_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/uranium"));
    public static final TagKey<Block> URANIUM_ORE_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/uranium"));

    // compat ores
    public static final TagKey<Item> TIN_RAW_MATERIALS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/tin"));
    public static final TagKey<Item> TIN_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/tin"));
    public static final TagKey<Item> TIN_DUSTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dusts/tin"));
    public static final TagKey<Item> TIN_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/tin"));
    public static final TagKey<Item> TIN_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/tin"));
    public static final TagKey<Item> TIN_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/tin"));
    public static final TagKey<Item> ZINC_RAW_MATERIALS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "raw_materials/zinc"));
    public static final TagKey<Item> ZINC_ORES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ores/zinc"));
    public static final TagKey<Item> ZINC_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/zinc"));
    public static final TagKey<Item> ZINC_NUGGETS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "nuggets/zinc"));
    public static final TagKey<Item> ZINC_CLUMPS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "clumps/zinc"));
    public static final TagKey<Item> BRONZE_INGOTS = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "ingots/bronze"));
    // also need osmium, silver, lead, quicksilver, bauxite
    
    // plastic
    public static final TagKey<Item> PLASTIC_PLATES = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "plates/plastic"));
    
    // biomass
    public static final TagKey<Item> BIOMASS = TagKey.of(RegistryKeys.ITEM, Oritech.id("biomass"));
    public static final TagKey<Item> BIOFUEL = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "fuels/bio"));
    public static final TagKey<Item> BIOFUEL_BLOCK = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "fuels/block/bio"));
    
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
    public static final TagKey<Block> LASER_ACCELERATED = TagKey.of(RegistryKeys.BLOCK, Oritech.id("laser_accelerated"));

    // trees
    public static final TagKey<Block> CUTTER_LOGS_MINEABLE = TagKey.of(RegistryKeys.BLOCK, Oritech.id("mineable/cutter_logs"));
    public static final TagKey<Block> CUTTER_LEAVES_MINEABLE = TagKey.of(RegistryKeys.BLOCK, Oritech.id("mineable/cutter_leaves"));
    
    // reactor
    public static final TagKey<Block> REACTOR_WALL_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Oritech.id("reactor_wall_blocks"));
    public static final TagKey<Item> REACTOR_COOLANT = TagKey.of(RegistryKeys.ITEM, Oritech.id("reactor_coolant_items"));
    
    // feeder blacklist
    public static final TagKey<Item> FEEDER_BLACKLIST = TagKey.of(RegistryKeys.ITEM, Oritech.id("feeder_blacklist"));

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
    
    // spawner blacklist
    public static final TagKey<EntityType<?>> SPAWNER_BLACKLIST = TagKey.of(RegistryKeys.ENTITY_TYPE, Oritech.id("spawner_blacklist"));
    
    // unstable container contents
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_LOW = TagKey.of(RegistryKeys.BLOCK, Oritech.id("unstable_container/low"));
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_MEDIUM = TagKey.of(RegistryKeys.BLOCK, Oritech.id("unstable_container/medium"));
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_HIGH = TagKey.of(RegistryKeys.BLOCK, Oritech.id("unstable_container/high"));
}
