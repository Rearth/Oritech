package rearth.oritech.init;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import rearth.oritech.Oritech;

public class TagContent {
    
    // only add tags for common stuff, such as dusts and nuggets. Gems are oritech-only
    // items
    // vanilla variants
    public static final TagKey<Item> CLUMPS = cItemTag("clumps");
    public static final TagKey<Item> DUSTS = cItemTag("dusts");

    public static final TagKey<Item> COPPER_CLUMPS = cItemTag("clumps/copper");
    public static final TagKey<Item> COPPER_DUSTS = cItemTag("dusts/copper");
    public static final TagKey<Item> COPPER_NUGGETS = cItemTag("nuggets/copper");
    
    public static final TagKey<Item> IRON_CLUMPS = cItemTag("clumps/iron");
    public static final TagKey<Item> IRON_DUSTS = cItemTag("dusts/iron");

    public static final TagKey<Item> GOLD_CLUMPS = cItemTag("clumps/gold");
    public static final TagKey<Item> GOLD_DUSTS = cItemTag("dusts/gold");
    
    public static final TagKey<Item> QUARTZ_DUSTS = cItemTag("dusts/quartz");
    public static final TagKey<Item> COAL_DUSTS = cItemTag("dusts/coal");

    // custom ores -----
    // nickel
    public static final TagKey<Block> NICKEL_ORE_BLOCKS = cBlockTag("ores/nickel");
    public static final TagKey<Item> NICKEL_ORES = cItemTag("ores/nickel");
    public static final TagKey<Item> NICKEL_RAW_MATERIALS = cItemTag("raw_materials/nickel");
    public static final TagKey<Item> NICKEL_CLUMPS = cItemTag("clumps/nickel");
    public static final TagKey<Item> NICKEL_DUSTS = cItemTag("dusts/nickel");
    public static final TagKey<Item> NICKEL_INGOTS = cItemTag("ingots/nickel");
    public static final TagKey<Item> NICKEL_NUGGETS = cItemTag("nuggets/nickel");
    
    // platinum
    public static final TagKey<Block> PLATINUM_ORE_BLOCKS = cBlockTag("ores/platinum");
    public static final TagKey<Item> PLATINUM_ORES = cItemTag("ores/platinum");
    public static final TagKey<Item> PLATINUM_RAW_MATERIALS = cItemTag("raw_materials/platinum");
    public static final TagKey<Item> PLATINUM_CLUMPS = cItemTag("clumps/platinum");
    public static final TagKey<Item> PLATINUM_DUSTS = cItemTag("dusts/platinum");
    public static final TagKey<Item> PLATINUM_INGOTS = cItemTag("ingots/platinum");
    public static final TagKey<Item> PLATINUM_NUGGETS = cItemTag("nuggets/platinum");
    
    // uranium
    public static final TagKey<Block> URANIUM_ORE_BLOCKS = cBlockTag("ores/uranium");
    public static final TagKey<Item> URANIUM_ORES = cItemTag("ores/uranium");
    public static final TagKey<Item> URANIUM_RAW_MATERIALS = cItemTag("raw_materials/uranium");
    public static final TagKey<Item> URANIUM_CLUMPS = cItemTag("clumps/uranium");
    public static final TagKey<Item> URANIUM_DUSTS = cItemTag("dusts/uranium");
    
    // plutonium
    public static final TagKey<Item> PLUTONIUM_DUSTS = cItemTag("dusts/plutonium");

    // alloys
    public static final TagKey<Item> ELECTRUM_INGOTS = cItemTag("ingots/electrum");
    public static final TagKey<Item> ELECTRUM_DUSTS = cItemTag("dusts/electrum");
    public static final TagKey<Item> STEEL_INGOTS = cItemTag("ingots/steel");

    // plating
    public static final TagKey<Item> MACHINE_PLATING = oritechItemTag("plating");

    // plastic
    public static final TagKey<Item> PLASTIC_PLATES = cItemTag("plates/plastic");
    
    // biomass
    // BIOMATTER is any plantlike item that can be pulverized into biomass
    public static final TagKey<Item> BIOMATTER = oritechItemTag("biomatter");
    // BIOMASS tag is for the biomass item and other similar items (Mekanism's bio fuel, Tech Reborn's plantball, etc.)
    public static final TagKey<Item> BIOMASS = cItemTag("fuels/bio");
    public static final TagKey<Item> BIOMASS_BLOCK = cItemTag("fuels/block/bio");

    // frame support
    public static final TagKey<Block> MACHINE_FRAME_SUPPORT = TagKey.of(RegistryKeys.BLOCK, Oritech.id("frame_support"));

    // silicon
    public static final TagKey<Item> SILICON = cItemTag("silicon");

    // carbon fibre
    public static final TagKey<Item> CARBON_FIBRE = cItemTag("carbon_fibre");

    // wires
    public static final TagKey<Item> WIRES = cItemTag("wires");
    
    // blocks
    public static final TagKey<Block> DRILL_MINEABLE = oritechBlockTag("mineable/drill");
    public static final TagKey<Block> RESOURCE_NODES = oritechBlockTag("resource_nodes");
    public static final TagKey<Block> LASER_PASSTHROUGH = oritechBlockTag("laser_passthrough");
    public static final TagKey<Block> LASER_ACCELERATED = oritechBlockTag("laser_accelerated");

    // trees
    public static final TagKey<Block> CUTTER_LOGS_MINEABLE = oritechBlockTag("mineable/cutter_logs");
    public static final TagKey<Block> CUTTER_LEAVES_MINEABLE = oritechBlockTag("mineable/cutter_leaves");
    
    // c stuff
    public static final TagKey<Block> CONVENTIONAL_ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));
    public static final TagKey<Item> CONVENTIONAL_FERTILIZER = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "fertilizers"));
    public static final TagKey<Biome> CONVENTIONAL_COLD = TagKey.of(RegistryKeys.BIOME, Identifier.of("c", "is_cold"));
    
    // reactor
    public static final TagKey<Block> REACTOR_WALL_BLOCKS = oritechBlockTag("reactor_wall_blocks");
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
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_LOW = oritechBlockTag("unstable_container/low");
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_MEDIUM = oritechBlockTag("unstable_container/medium");
    public static final TagKey<Block> UNSTABLE_CONTAINER_SOURCES_HIGH = oritechBlockTag("unstable_container/high");

    // recyclables
    public static final TagKey<Item> RECYCLES_TO_NETHERITE_SCRAP = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/netherite_scrap"));
    public static final TagKey<Item> RECYCLES_TO_DIAMOND = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/diamond"));
    public static final TagKey<Item> RECYCLES_TO_IRON_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/iron_dust"));
    public static final TagKey<Item> RECYCLES_TO_SMALL_IRON_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/small_iron_dust"));
    public static final TagKey<Item> RECYCLES_TO_GOLD_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/gold_dust"));
    public static final TagKey<Item> RECYCLES_TO_SMALL_GOLD_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/small_gold_dust"));
    public static final TagKey<Item> RECYCLES_TO_COPPER_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/copper_dust"));
    public static final TagKey<Item> RECYCLES_TO_SMALL_COPPER_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/small_copper_dust"));
    public static final TagKey<Item> RECYCLES_TO_2_QUARTZ_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/quartz_dust_2"));
    public static final TagKey<Item> RECYCLES_TO_4_QUARTZ_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/quartz_dust_4"));
    public static final TagKey<Item> RECYCLES_TO_REDSTONE_DUST = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/redstone_dust"));
    public static final TagKey<Item> RECYCLES_TO_GRAVEL = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/gravel"));
    public static final TagKey<Item> RECYCLES_TO_SAND = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/sand"));
    public static final TagKey<Item> RECYCLES_TO_RED_SAND = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/red_sand"));
    public static final TagKey<Item> RECYCLES_TO_STRING = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/string"));
    public static final TagKey<Item> RECYCLES_TO_BIOMASS = TagKey.of(RegistryKeys.ITEM, Oritech.id("recyclable/biomass"));

    public static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(namespace, path));
    }

    public static TagKey<Item> cItemTag(String path) {
        return itemTag("c", path);
    }

    public static TagKey<Block> cBlockTag(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", path));
    }

    private static TagKey<Item> oritechItemTag(String path) {
        return TagKey.of(RegistryKeys.ITEM, Oritech.id(path));
    }

    private static TagKey<Block> oritechBlockTag(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Oritech.id(path));
    }
}
