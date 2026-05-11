package rearth.oritech.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import oshi.util.tuples.Pair;
import rearth.oritech.Oritech;
import rearth.oritech.item.UnstableContainerItem;
import rearth.oritech.item.other.ColorCartridgeItem;
import rearth.oritech.item.other.CustomTooltipItem;
import rearth.oritech.item.other.MobCaptureItem;
import rearth.oritech.item.other.SmallFluidTankBlockItem;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.item.tools.WeedKiller;
import rearth.oritech.item.tools.Wrench;
import rearth.oritech.util.ColorableMachine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("NullableProblems")
public class ItemContent {
    
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Oritech.MOD_ID);
    
    @ItemGroupTarget(Groups.COMPONENTS)
    @Compostable(0.65F)
    public static final DeferredItem<Item> BANANA = ITEMS.registerItem("banana", Item::new, props -> props.food(Foods.APPLE));
    @ItemGroupTarget(Groups.EQUIPMENT)
    public static final DeferredItem<Item> TARGET_DESIGNATOR = ITEMS.registerItem("target_designator", LaserTargetDesignator::new, props -> props.stacksTo(1));
    @ItemGroupTarget(Groups.EQUIPMENT)
    public static final DeferredItem<Item> WEED_KILLER = ITEMS.registerItem("weed_killer", WeedKiller::new, props -> props.stacksTo(1));
    @ItemGroupTarget(Groups.EQUIPMENT)
    public static final DeferredItem<Item> WRENCH = ITEMS.registerItem("wrench", Wrench::new, props -> props.stacksTo(1).component(DataComponents.TOOL, Wrench.createToolComponent()));

    // region metals
    // nickel
    public static final DeferredItem<Item> NICKEL_INGOT = ITEMS.registerSimpleItem("nickel_ingot");
    public static final DeferredItem<Item> RAW_NICKEL = ITEMS.registerSimpleItem("raw_nickel");
    public static final DeferredItem<Item> NICKEL_CLUMP = ITEMS.registerSimpleItem("nickel_clump");
    public static final DeferredItem<Item> SMALL_NICKEL_CLUMP = ITEMS.registerSimpleItem("small_nickel_clump");
    public static final DeferredItem<Item> NICKEL_DUST = ITEMS.registerSimpleItem("nickel_dust");
    public static final DeferredItem<Item> SMALL_NICKEL_DUST = ITEMS.registerSimpleItem("small_nickel_dust");
    public static final DeferredItem<Item> NICKEL_GEM = ITEMS.registerSimpleItem("nickel_gem");
    public static final DeferredItem<Item> NICKEL_NUGGET = ITEMS.registerSimpleItem("nickel_nugget");
    // platinum
    public static final DeferredItem<Item> PLATINUM_INGOT = ITEMS.registerSimpleItem("platinum_ingot");
    public static final DeferredItem<Item> RAW_PLATINUM = ITEMS.registerSimpleItem("raw_platinum");
    public static final DeferredItem<Item> PLATINUM_CLUMP = ITEMS.registerSimpleItem("platinum_clump");
    public static final DeferredItem<Item> SMALL_PLATINUM_CLUMP = ITEMS.registerSimpleItem("small_platinum_clump");
    public static final DeferredItem<Item> PLATINUM_DUST = ITEMS.registerSimpleItem("platinum_dust");
    public static final DeferredItem<Item> SMALL_PLATINUM_DUST = ITEMS.registerSimpleItem("small_platinum_dust");
    public static final DeferredItem<Item> PLATINUM_GEM = ITEMS.registerSimpleItem("platinum_gem");
    public static final DeferredItem<Item> PLATINUM_NUGGET = ITEMS.registerSimpleItem("platinum_nugget");
    // iron
    public static final DeferredItem<Item> IRON_CLUMP = ITEMS.registerSimpleItem("iron_clump");
    public static final DeferredItem<Item> SMALL_IRON_CLUMP = ITEMS.registerSimpleItem("small_iron_clump");
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> SMALL_IRON_DUST = ITEMS.registerSimpleItem("small_iron_dust");
    public static final DeferredItem<Item> IRON_GEM = ITEMS.registerSimpleItem("iron_gem");
    // copper
    public static final DeferredItem<Item> COPPER_CLUMP = ITEMS.registerSimpleItem("copper_clump");
    public static final DeferredItem<Item> SMALL_COPPER_CLUMP = ITEMS.registerSimpleItem("small_copper_clump");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> SMALL_COPPER_DUST = ITEMS.registerSimpleItem("small_copper_dust");
    public static final DeferredItem<Item> COPPER_GEM = ITEMS.registerSimpleItem("copper_gem");
    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget");
    // gold
    public static final DeferredItem<Item> GOLD_CLUMP = ITEMS.registerSimpleItem("gold_clump");
    public static final DeferredItem<Item> SMALL_GOLD_CLUMP = ITEMS.registerSimpleItem("small_gold_clump");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> SMALL_GOLD_DUST = ITEMS.registerSimpleItem("small_gold_dust");
    public static final DeferredItem<Item> GOLD_GEM = ITEMS.registerSimpleItem("gold_gem");
    // alloys
    public static final DeferredItem<Item> FLUXITE = ITEMS.registerItem("fluxite", props -> new CustomTooltipItem(props, "tooltip.oritech.fluxite"));
    public static final DeferredItem<Item> ADAMANT_INGOT = ITEMS.registerSimpleItem("adamant_ingot");
    public static final DeferredItem<Item> ADAMANT_DUST = ITEMS.registerSimpleItem("adamant_dust");
    public static final DeferredItem<Item> BIOSTEEL_INGOT = ITEMS.registerSimpleItem("biosteel_ingot");
    public static final DeferredItem<Item> BIOSTEEL_DUST = ITEMS.registerSimpleItem("biosteel_dust");
    public static final DeferredItem<Item> DURATIUM_INGOT = ITEMS.registerSimpleItem("duratium_ingot");
    public static final DeferredItem<Item> DURATIUM_DUST = ITEMS.registerSimpleItem("duratium_dust");
    public static final DeferredItem<Item> ELECTRUM_INGOT = ITEMS.registerSimpleItem("electrum_ingot");
    public static final DeferredItem<Item> ELECTRUM_DUST = ITEMS.registerSimpleItem("electrum_dust");
    public static final DeferredItem<Item> ENERGITE_INGOT = ITEMS.registerSimpleItem("energite_ingot");
    public static final DeferredItem<Item> ENERGITE_DUST = ITEMS.registerSimpleItem("energite_dust");
    public static final DeferredItem<Item> PROMETHEUM_INGOT = ITEMS.registerItem("prometheum_ingot", Item::new, props -> props.rarity(Rarity.EPIC));
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> STEEL_DUST = ITEMS.registerSimpleItem("steel_dust");
    //endregion
    
    // region crafting components
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> CARBON_FIBRE_STRANDS = ITEMS.registerSimpleItem("carbon_fibre_strands");
    public static final DeferredItem<Item> ENDERIC_COMPOUND = ITEMS.registerSimpleItem("enderic_compound");
    public static final DeferredItem<Item> MAGNETIC_COIL = ITEMS.registerSimpleItem("magnetic_coil");
    public static final DeferredItem<Item> CLAY_CATALYST_BEADS = ITEMS.registerSimpleItem("clay_catalyst_beads");
    public static final DeferredItem<Item> MOTOR = ITEMS.registerItem("motor", props -> new CustomTooltipItem(props, "tooltip.oritech.motor"));
    public static final DeferredItem<Item> BASIC_BATTERY = ITEMS.registerSimpleItem("basic_battery");
    public static final DeferredItem<Item> RAW_SILICON = ITEMS.registerSimpleItem("raw_silicon");
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon");
    public static final DeferredItem<Item> RAW_BIOPOLYMER = ITEMS.registerSimpleItem("raw_biopolymer");
    public static final DeferredItem<Item> POLYMER_RESIN = ITEMS.registerSimpleItem("polymer_resin");
    public static final DeferredItem<Item> PLASTIC_SHEET = ITEMS.registerSimpleItem("plastic_sheet");
    public static final DeferredItem<Item> PROCESSING_UNIT = ITEMS.registerSimpleItem("processing_unit");
    public static final DeferredItem<Item> REINFORCED_CARBON_SHEET = ITEMS.registerSimpleItem("reinforced_carbon_sheet");
    public static final DeferredItem<Item> ION_THRUSTER = ITEMS.registerSimpleItem("ion_thruster");
    public static final DeferredItem<Item> ADVANCED_COMPUTING_ENGINE = ITEMS.registerSimpleItem("advanced_computing_engine");
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.registerSimpleItem("silicon_wafer");
    public static final DeferredItem<Item> DUBIOS_CONTAINER = ITEMS.registerItem("dubios_container", props -> new MobCaptureItem(props.stacksTo(16), List.of(EntityType.VEX, EntityType.ALLAY, EntityType.PHANTOM)));
    public static final DeferredItem<Item> ENDERIC_LENS = ITEMS.registerSimpleItem("enderic_lens");
    public static final DeferredItem<Item> FLUX_GATE = ITEMS.registerSimpleItem("flux_gate");
    public static final DeferredItem<Item> ADVANCED_BATTERY = ITEMS.registerSimpleItem("advanced_battery");
    public static final DeferredItem<Item> SUPER_AI_CHIP = ITEMS.registerItem("super_ai_chip", Item::new, props -> props.stacksTo(4));
    public static final DeferredItem<Item> UNHOLY_INTELLIGENCE = ITEMS.registerItem("unholy_intelligence", props -> new CustomTooltipItem(props.stacksTo(1), "tooltip.oritech.intelligence_item"));
    public static final DeferredItem<Item> HEISENBERG_COMPENSATOR = ITEMS.registerSimpleItem("heisenberg_compensator");
    public static final DeferredItem<Item> OVERCHARGED_CRYSTAL = ITEMS.registerItem("overcharged_crystal", props -> new CustomTooltipItem(props.stacksTo(1), "tooltip.oritech.overchargedcrystal"));
    @Compostable(1.0F)
    public static final DeferredItem<Item> PACKED_WHEAT = ITEMS.registerSimpleItem("packed_wheat");
    public static final DeferredItem<Item> QUARTZ_DUST = ITEMS.registerSimpleItem("quartz_dust");
    public static final DeferredItem<Item> UNSTABLE_CONTAINER = ITEMS.registerItem("unstable_container", props -> new UnstableContainerItem(props.stacksTo(1), 0.23f, "unstable_container"));
    
    // bio
    @Compostable(0.3F)
    public static final DeferredItem<Item> BIOMASS = ITEMS.registerItem("biomass", Item::new, props -> props.food(Foods.POISONOUS_POTATO));
    public static final DeferredItem<Item> SOLID_BIOFUEL = ITEMS.registerSimpleItem("solid_biofuel");
    
    // reactor items
    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.registerSimpleItem("raw_uranium");
    public static final DeferredItem<Item> URANIUM_GEM = ITEMS.registerSimpleItem("uranium_gem");
    public static final DeferredItem<Item> SMALL_URANIUM_DUST = ITEMS.registerSimpleItem("small_uranium_dust");
    public static final DeferredItem<Item> URANIUM_DUST = ITEMS.registerSimpleItem("uranium_dust");
    public static final DeferredItem<Item> SMALL_PLUTONIUM_DUST = ITEMS.registerSimpleItem("small_plutonium_dust");
    public static final DeferredItem<Item> PLUTONIUM_DUST = ITEMS.registerSimpleItem("plutonium_dust");
    public static final DeferredItem<Item> SMALL_URANIUM_PELLET = ITEMS.registerItem("small_uranium_pellet", props -> new CustomTooltipItem(props, "tooltip.oritech.small_uranium_pellet"));
    public static final DeferredItem<Item> URANIUM_PELLET = ITEMS.registerItem("uranium_pellet", props -> new CustomTooltipItem(props, "tooltip.oritech.uranium_pellet"));
    public static final DeferredItem<Item> SMALL_PLUTONIUM_PELLET = ITEMS.registerItem("small_plutonium_pellet", props -> new CustomTooltipItem(props, "tooltip.oritech.small_plutonium_pellet"));
    public static final DeferredItem<Item> PLUTONIUM_PELLET = ITEMS.registerItem("plutonium_pellet", props -> new CustomTooltipItem(props, "tooltip.oritech.plutonium_pellet"));
    
    // colors
    public static final DeferredItem<Item> DIAMOND_PAINT = ITEMS.registerItem("diamond_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.DIAMOND));
    public static final DeferredItem<Item> REDSTONE_PAINT = ITEMS.registerItem("redstone_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.REDSTONE));
    public static final DeferredItem<Item> ORANGE_PAINT = ITEMS.registerItem("orange_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.ORANGE));
    public static final DeferredItem<Item> CAMO_PAINT = ITEMS.registerItem("camo_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.CAMO));
    public static final DeferredItem<Item> FLUXITE_PAINT = ITEMS.registerItem("fluxite_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.FLUXITE));
    public static final DeferredItem<Item> WHITE_PAINT = ITEMS.registerItem("white_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.WHITE));
    public static final DeferredItem<Item> INDUSTRIAL_PAINT = ITEMS.registerItem("industrial_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.INDUSTRIAL));
    public static final DeferredItem<Item> NETHERITE_PAINT = ITEMS.registerItem("netherite_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.NETHERITE));
    public static final DeferredItem<Item> SCULK_PAINT = ITEMS.registerItem("sculk_paint", props -> new ColorCartridgeItem(props, ColorableMachine.ColorVariant.SCULK));

    // tank items (with custom item class)
    public static final DeferredItem<Item> SMALL_TANK_ITEM = ITEMS.registerItem("small_tank_block", props -> new SmallFluidTankBlockItem(BlockContent.SMALL_TANK_BLOCK.value(), props.useBlockDescriptionPrefix()));
    public static final DeferredItem<Item> CREATIVE_TANK_ITEM = ITEMS.registerItem("small_tank_block", props -> new SmallFluidTankBlockItem(BlockContent.CREATIVE_TANK_BLOCK.value(), props.useBlockDescriptionPrefix()));
    
    
    public enum Groups {
        MACHINES, COMPONENTS, EQUIPMENT, DECORATIVE, NONE
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface NoModelGeneration {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface ItemGroupTarget {
        Groups value();
    }

    // todo (both here and in blockcontent)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Compostable {
        float value();
    }
}
