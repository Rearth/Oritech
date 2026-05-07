package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import oshi.util.tuples.Pair;
import rearth.oritech.Oritech;
import rearth.oritech.item.UnstableContainerItem;
import rearth.oritech.item.other.ColorCartridgeItem;
import rearth.oritech.item.other.CustomTooltipItem;
import rearth.oritech.item.other.MobCaptureItem;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.item.tools.WeedKiller;
import rearth.oritech.item.tools.Wrench;
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.util.registry.OritechItemRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemContent {
    
    public static final OritechItemRegistry ITEMS = new OritechItemRegistry();
    private static final Map<RegistrySupplier<? extends Item>, Item> ITEM_VALUES = new IdentityHashMap<>();
    private static boolean loaded;
    
    
    public static Set<Item> autoRegisteredModels = new HashSet<>();

    public static void load() {
        if (loaded) return;

        loaded = true;
        for (var field : ItemContent.class.getDeclaredFields()) {
            if (!RegistrySupplier.class.isAssignableFrom(field.getType())) continue;

            try {
                field.setAccessible(true);
                var supplier = (RegistrySupplier<? extends Item>) field.get(null);
                var item = ITEM_VALUES.get(supplier);
                if (item == null) continue;

                postProcessField(item, supplier.getId(), field, supplier);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access item field: " + field.getName(), e);
            }
        }
    }

    public static void registerItems() {
        FluidContent.registerItems();
        ToolsContent.register();
        BlockContent.load();
        ITEMS.register();
        load();
    }

    public static <T extends Item> RegistrySupplier<T> registerItem(String path, T item) {
        var supplier = ITEMS.register(path, () -> item);
        ITEM_VALUES.put(supplier, item);
        return supplier;
    }

    public static Item itemValue(RegistrySupplier<? extends Item> supplier) {
        return ITEM_VALUES.get(supplier);
    }
    
    @ItemGroupTarget(Groups.components)
    @Compostable(0.65F)
    public static final RegistrySupplier<Item> BANANA = registerItem("banana", new Item(itemProperties("banana").food(Foods.APPLE)));
    @ItemGroupTarget(Groups.equipment)
    public static final RegistrySupplier<Item> TARGET_DESIGNATOR = registerItem("target_designator", new LaserTargetDesignator(itemProperties("target_designator").stacksTo(1)));
    @ItemGroupTarget(Groups.equipment)
    public static final RegistrySupplier<Item> WEED_KILLER = registerItem("weed_killer", new WeedKiller(itemProperties("weed_killer").stacksTo(1)));
    @ItemGroupTarget(Groups.equipment)
    public static final RegistrySupplier<Item> WRENCH = registerItem("wrench", new Wrench(itemProperties("wrench").stacksTo(1).component(DataComponents.TOOL, Wrench.createToolComponent())));

    // region metals
    // nickel
    public static final RegistrySupplier<Item> NICKEL_INGOT = registerItem("nickel_ingot", new Item(itemProperties("nickel_ingot")));
    public static final RegistrySupplier<Item> RAW_NICKEL = registerItem("raw_nickel", new Item(itemProperties("raw_nickel")));
    public static final RegistrySupplier<Item> NICKEL_CLUMP = registerItem("nickel_clump", new Item(itemProperties("nickel_clump")));
    public static final RegistrySupplier<Item> SMALL_NICKEL_CLUMP = registerItem("small_nickel_clump", new Item(itemProperties("small_nickel_clump")));
    public static final RegistrySupplier<Item> NICKEL_DUST = registerItem("nickel_dust", new Item(itemProperties("nickel_dust")));
    public static final RegistrySupplier<Item> SMALL_NICKEL_DUST = registerItem("small_nickel_dust", new Item(itemProperties("small_nickel_dust")));
    public static final RegistrySupplier<Item> NICKEL_GEM = registerItem("nickel_gem", new Item(itemProperties("nickel_gem")));
    public static final RegistrySupplier<Item> NICKEL_NUGGET = registerItem("nickel_nugget", new Item(itemProperties("nickel_nugget")));
    // platinum
    public static final RegistrySupplier<Item> PLATINUM_INGOT = registerItem("platinum_ingot", new Item(itemProperties("platinum_ingot")));
    public static final RegistrySupplier<Item> RAW_PLATINUM = registerItem("raw_platinum", new Item(itemProperties("raw_platinum")));
    public static final RegistrySupplier<Item> PLATINUM_CLUMP = registerItem("platinum_clump", new Item(itemProperties("platinum_clump")));
    public static final RegistrySupplier<Item> SMALL_PLATINUM_CLUMP = registerItem("small_platinum_clump", new Item(itemProperties("small_platinum_clump")));
    public static final RegistrySupplier<Item> PLATINUM_DUST = registerItem("platinum_dust", new Item(itemProperties("platinum_dust")));
    public static final RegistrySupplier<Item> SMALL_PLATINUM_DUST = registerItem("small_platinum_dust", new Item(itemProperties("small_platinum_dust")));
    public static final RegistrySupplier<Item> PLATINUM_GEM = registerItem("platinum_gem", new Item(itemProperties("platinum_gem")));
    public static final RegistrySupplier<Item> PLATINUM_NUGGET = registerItem("platinum_nugget", new Item(itemProperties("platinum_nugget")));
    // iron
    public static final RegistrySupplier<Item> IRON_CLUMP = registerItem("iron_clump", new Item(itemProperties("iron_clump")));
    public static final RegistrySupplier<Item> SMALL_IRON_CLUMP = registerItem("small_iron_clump", new Item(itemProperties("small_iron_clump")));
    public static final RegistrySupplier<Item> IRON_DUST = registerItem("iron_dust", new Item(itemProperties("iron_dust")));
    public static final RegistrySupplier<Item> SMALL_IRON_DUST = registerItem("small_iron_dust", new Item(itemProperties("small_iron_dust")));
    public static final RegistrySupplier<Item> IRON_GEM = registerItem("iron_gem", new Item(itemProperties("iron_gem")));
    // copper
    public static final RegistrySupplier<Item> COPPER_CLUMP = registerItem("copper_clump", new Item(itemProperties("copper_clump")));
    public static final RegistrySupplier<Item> SMALL_COPPER_CLUMP = registerItem("small_copper_clump", new Item(itemProperties("small_copper_clump")));
    public static final RegistrySupplier<Item> COPPER_DUST = registerItem("copper_dust", new Item(itemProperties("copper_dust")));
    public static final RegistrySupplier<Item> SMALL_COPPER_DUST = registerItem("small_copper_dust", new Item(itemProperties("small_copper_dust")));
    public static final RegistrySupplier<Item> COPPER_GEM = registerItem("copper_gem", new Item(itemProperties("copper_gem")));
    public static final RegistrySupplier<Item> COPPER_NUGGET = registerItem("copper_nugget", new Item(itemProperties("copper_nugget")));
    // gold
    public static final RegistrySupplier<Item> GOLD_CLUMP = registerItem("gold_clump", new Item(itemProperties("gold_clump")));
    public static final RegistrySupplier<Item> SMALL_GOLD_CLUMP = registerItem("small_gold_clump", new Item(itemProperties("small_gold_clump")));
    public static final RegistrySupplier<Item> GOLD_DUST = registerItem("gold_dust", new Item(itemProperties("gold_dust")));
    public static final RegistrySupplier<Item> SMALL_GOLD_DUST = registerItem("small_gold_dust", new Item(itemProperties("small_gold_dust")));
    public static final RegistrySupplier<Item> GOLD_GEM = registerItem("gold_gem", new Item(itemProperties("gold_gem")));
    // alloys
    public static final RegistrySupplier<Item> FLUXITE = registerItem("fluxite", new CustomTooltipItem(itemProperties("fluxite"), "tooltip.oritech.fluxite"));
    public static final RegistrySupplier<Item> ADAMANT_INGOT = registerItem("adamant_ingot", new Item(itemProperties("adamant_ingot")));
    public static final RegistrySupplier<Item> ADAMANT_DUST = registerItem("adamant_dust", new Item(itemProperties("adamant_dust")));
    public static final RegistrySupplier<Item> BIOSTEEL_INGOT = registerItem("biosteel_ingot", new Item(itemProperties("biosteel_ingot")));
    public static final RegistrySupplier<Item> BIOSTEEL_DUST = registerItem("biosteel_dust", new Item(itemProperties("biosteel_dust")));
    public static final RegistrySupplier<Item> DURATIUM_INGOT = registerItem("duratium_ingot", new Item(itemProperties("duratium_ingot")));
    public static final RegistrySupplier<Item> DURATIUM_DUST = registerItem("duratium_dust", new Item(itemProperties("duratium_dust")));
    public static final RegistrySupplier<Item> ELECTRUM_INGOT = registerItem("electrum_ingot", new Item(itemProperties("electrum_ingot")));
    public static final RegistrySupplier<Item> ELECTRUM_DUST = registerItem("electrum_dust", new Item(itemProperties("electrum_dust")));
    public static final RegistrySupplier<Item> ENERGITE_INGOT = registerItem("energite_ingot", new Item(itemProperties("energite_ingot")));
    public static final RegistrySupplier<Item> ENERGITE_DUST = registerItem("energite_dust", new Item(itemProperties("energite_dust")));
    public static final RegistrySupplier<Item> PROMETHEUM_INGOT = registerItem("prometheum_ingot", new Item(itemProperties("prometheum_ingot").rarity(Rarity.EPIC)));
    public static final RegistrySupplier<Item> STEEL_INGOT = registerItem("steel_ingot", new Item(itemProperties("steel_ingot")));
    public static final RegistrySupplier<Item> STEEL_DUST = registerItem("steel_dust", new Item(itemProperties("steel_dust")));
    //endregion
    
    // region crafting components
    public static final RegistrySupplier<Item> COAL_DUST = registerItem("coal_dust", new Item(itemProperties("coal_dust")));
    public static final RegistrySupplier<Item> CARBON_FIBRE_STRANDS = registerItem("carbon_fibre_strands", new Item(itemProperties("carbon_fibre_strands")));
    public static final RegistrySupplier<Item> ENDERIC_COMPOUND = registerItem("enderic_compound", new Item(itemProperties("enderic_compound")));
    public static final RegistrySupplier<Item> MAGNETIC_COIL = registerItem("magnetic_coil", new Item(itemProperties("magnetic_coil")));
    public static final RegistrySupplier<Item> CLAY_CATALYST_BEADS = registerItem("clay_catalyst_beads", new Item(itemProperties("clay_catalyst_beads")));
    public static final RegistrySupplier<Item> MOTOR = registerItem("motor", new CustomTooltipItem(itemProperties("motor"), "tooltip.oritech.motor"));
    public static final RegistrySupplier<Item> BASIC_BATTERY = registerItem("basic_battery", new Item(itemProperties("basic_battery")));
    public static final RegistrySupplier<Item> RAW_SILICON = registerItem("raw_silicon", new Item(itemProperties("raw_silicon")));
    public static final RegistrySupplier<Item> SILICON = registerItem("silicon", new Item(itemProperties("silicon")));
    public static final RegistrySupplier<Item> RAW_BIOPOLYMER = registerItem("raw_biopolymer", new Item(itemProperties("raw_biopolymer")));
    public static final RegistrySupplier<Item> POLYMER_RESIN = registerItem("polymer_resin", new Item(itemProperties("polymer_resin")));
    public static final RegistrySupplier<Item> PLASTIC_SHEET = registerItem("plastic_sheet", new Item(itemProperties("plastic_sheet")));
    public static final RegistrySupplier<Item> PROCESSING_UNIT = registerItem("processing_unit", new Item(itemProperties("processing_unit")));
    public static final RegistrySupplier<Item> REINFORCED_CARBON_SHEET = registerItem("reinforced_carbon_sheet", new Item(itemProperties("reinforced_carbon_sheet")));
    public static final RegistrySupplier<Item> ION_THRUSTER = registerItem("ion_thruster", new Item(itemProperties("ion_thruster")));
    public static final RegistrySupplier<Item> ADVANCED_COMPUTING_ENGINE = registerItem("advanced_computing_engine", new Item(itemProperties("advanced_computing_engine")));
    public static final RegistrySupplier<Item> SILICON_WAFER = registerItem("silicon_wafer", new Item(itemProperties("silicon_wafer")));
    public static final RegistrySupplier<Item> DUBIOS_CONTAINER = registerItem("dubios_container", new MobCaptureItem(itemProperties("dubios_container").stacksTo(16), List.of(EntityType.VEX, EntityType.ALLAY, EntityType.PHANTOM)));
    public static final RegistrySupplier<Item> ENDERIC_LENS = registerItem("enderic_lens", new Item(itemProperties("enderic_lens")));
    public static final RegistrySupplier<Item> FLUX_GATE = registerItem("flux_gate", new Item(itemProperties("flux_gate")));
    public static final RegistrySupplier<Item> ADVANCED_BATTERY = registerItem("advanced_battery", new Item(itemProperties("advanced_battery")));
    public static final RegistrySupplier<Item> SUPER_AI_CHIP = registerItem("super_ai_chip", new Item(itemProperties("super_ai_chip").stacksTo(4)));
    public static final RegistrySupplier<Item> UNHOLY_INTELLIGENCE = registerItem("unholy_intelligence", new CustomTooltipItem(itemProperties("unholy_intelligence").stacksTo(1), "tooltip.oritech.intelligence_item"));
    public static final RegistrySupplier<Item> HEISENBERG_COMPENSATOR = registerItem("heisenberg_compensator", new Item(itemProperties("heisenberg_compensator")));
    public static final RegistrySupplier<Item> OVERCHARGED_CRYSTAL = registerItem("overcharged_crystal", new CustomTooltipItem(itemProperties("overcharged_crystal").stacksTo(1), "tooltip.oritech.overchargedcrystal"));
    @Compostable(1.0F)
    public static final RegistrySupplier<Item> PACKED_WHEAT = registerItem("packed_wheat", new Item(itemProperties("packed_wheat")));
    public static final RegistrySupplier<Item> QUARTZ_DUST = registerItem("quartz_dust", new Item(itemProperties("quartz_dust")));
    public static final RegistrySupplier<Item> UNSTABLE_CONTAINER = registerItem("unstable_container", new UnstableContainerItem(itemProperties("unstable_container").stacksTo(1), 0.23f, "unstable_container"));
    
    // bio
    @Compostable(0.3F)
    public static final RegistrySupplier<Item> BIOMASS = registerItem("biomass", new Item(itemProperties("biomass").food(Foods.POISONOUS_POTATO)));
    public static final RegistrySupplier<Item> SOLID_BIOFUEL = registerItem("solid_biofuel", new Item(itemProperties("solid_biofuel")));
    
    // reactor items
    public static final RegistrySupplier<Item> RAW_URANIUM = registerItem("raw_uranium", new Item(itemProperties("raw_uranium")));
    public static final RegistrySupplier<Item> URANIUM_GEM = registerItem("uranium_gem", new Item(itemProperties("uranium_gem")));
    public static final RegistrySupplier<Item> SMALL_URANIUM_DUST = registerItem("small_uranium_dust", new Item(itemProperties("small_uranium_dust")));
    public static final RegistrySupplier<Item> URANIUM_DUST = registerItem("uranium_dust", new Item(itemProperties("uranium_dust")));
    public static final RegistrySupplier<Item> SMALL_PLUTONIUM_DUST = registerItem("small_plutonium_dust", new Item(itemProperties("small_plutonium_dust")));
    public static final RegistrySupplier<Item> PLUTONIUM_DUST = registerItem("plutonium_dust", new Item(itemProperties("plutonium_dust")));
    public static final RegistrySupplier<Item> SMALL_URANIUM_PELLET = registerItem("small_uranium_pellet", new CustomTooltipItem(itemProperties("small_uranium_pellet"), "tooltip.oritech.small_uranium_pellet"));
    public static final RegistrySupplier<Item> URANIUM_PELLET = registerItem("uranium_pellet", new CustomTooltipItem(itemProperties("uranium_pellet"), "tooltip.oritech.uranium_pellet"));
    public static final RegistrySupplier<Item> SMALL_PLUTONIUM_PELLET = registerItem("small_plutonium_pellet", new CustomTooltipItem(itemProperties("small_plutonium_pellet"), "tooltip.oritech.small_plutonium_pellet"));
    public static final RegistrySupplier<Item> PLUTONIUM_PELLET = registerItem("plutonium_pellet", new CustomTooltipItem(itemProperties("plutonium_pellet"), "tooltip.oritech.plutonium_pellet"));
    
    // colors
    public static final RegistrySupplier<Item> DIAMOND_PAINT = registerItem("diamond_paint", new ColorCartridgeItem(itemProperties("diamond_paint"), ColorableMachine.ColorVariant.DIAMOND));
    public static final RegistrySupplier<Item> REDSTONE_PAINT = registerItem("redstone_paint", new ColorCartridgeItem(itemProperties("redstone_paint"), ColorableMachine.ColorVariant.REDSTONE));
    public static final RegistrySupplier<Item> ORANGE_PAINT = registerItem("orange_paint", new ColorCartridgeItem(itemProperties("orange_paint"), ColorableMachine.ColorVariant.ORANGE));
    public static final RegistrySupplier<Item> CAMO_PAINT = registerItem("camo_paint", new ColorCartridgeItem(itemProperties("camo_paint"), ColorableMachine.ColorVariant.CAMO));
    public static final RegistrySupplier<Item> FLUXITE_PAINT = registerItem("fluxite_paint", new ColorCartridgeItem(itemProperties("fluxite_paint"), ColorableMachine.ColorVariant.FLUXITE));
    public static final RegistrySupplier<Item> WHITE_PAINT = registerItem("white_paint", new ColorCartridgeItem(itemProperties("white_paint"), ColorableMachine.ColorVariant.WHITE));
    public static final RegistrySupplier<Item> INDUSTRIAL_PAINT = registerItem("industrial_paint", new ColorCartridgeItem(itemProperties("industrial_paint"), ColorableMachine.ColorVariant.INDUSTRIAL));
    public static final RegistrySupplier<Item> NETHERITE_PAINT = registerItem("netherite_paint", new ColorCartridgeItem(itemProperties("netherite_paint"), ColorableMachine.ColorVariant.NETHERITE));
    public static final RegistrySupplier<Item> SCULK_PAINT = registerItem("sculk_paint", new ColorCartridgeItem(itemProperties("sculk_paint"), ColorableMachine.ColorVariant.SCULK));

    private static void postProcessField(Item value, Identifier identifier, Field field, RegistrySupplier<? extends Item> supplier) {

        var targetGroup = Groups.components;
        if (field.isAnnotationPresent(ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemGroupTarget.class).value();
        }
        
        if (!field.isAnnotationPresent(NoModelGeneration.class)) {
            autoRegisteredModels.add(value);
        }

        if (field.isAnnotationPresent(Compostable.class)) {
            Oritech.COMPOSTABLES_DATA.add(new Pair<>(value, field.getAnnotation(Compostable.class).value()));
        }

        ItemGroups.add(targetGroup, value);
    }
    
    public enum Groups {
        machines, components, equipment, decorative, none
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface Compostable {
        float value();
    }

    private static Item.Properties itemProperties(String path) {
        return ITEMS.properties(path);
    }
}
