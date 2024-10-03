package rearth.oritech.init;

import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.owo.registration.annotations.IterationIgnored;
import io.wispforest.owo.registration.reflect.ItemRegistryContainer;
import net.minecraft.block.ComposterBlock;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import rearth.oritech.Oritech;
import rearth.oritech.init.datagen.ModelGenerator;
import rearth.oritech.item.other.CustomTooltipItem;
import rearth.oritech.item.other.MobCaptureItem;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.item.tools.WeedKiller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;

public class ItemContent implements ItemRegistryContainer {

    @ItemGroupTarget(Groups.components)
    @Compostable(0.65F)
    public static final Item BANANA = new Item(new Item.Settings().food(FoodComponents.APPLE));
    @ItemGroupTarget(Groups.equipment)
    public static final Item TARGET_DESIGNATOR = new LaserTargetDesignator(new Item.Settings().maxCount(1));
    @IterationIgnored
    public static final Item ORITECH_GUIDE = LavenderBookItem.registerForBook(Oritech.id("oritech_guide"), new Item.Settings().maxCount(1));
    @ItemGroupTarget(Groups.equipment)
    public static final Item WEED_KILLER = new WeedKiller(new Item.Settings().maxCount(1));
    public static final Item OIL_BUCKET = new BucketItem(FluidContent.STILL_OIL, new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1));
    public static final Item FUEL_BUCKET = new BucketItem(FluidContent.STILL_FUEL, new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1));
    public static final Item STEAM_BUCKET = new BucketItem(FluidContent.STILL_STEAM, new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1));
    
    // region metals
    // nickel
    public static final Item NICKEL_INGOT = new Item(new Item.Settings());
    public static final Item RAW_NICKEL = new Item(new Item.Settings());
    public static final Item NICKEL_CLUMP = new Item(new Item.Settings());
    public static final Item SMALL_NICKEL_CLUMP = new Item(new Item.Settings());
    public static final Item NICKEL_DUST = new Item(new Item.Settings());
    public static final Item SMALL_NICKEL_DUST = new Item(new Item.Settings());
    public static final Item NICKEL_GEM = new Item(new Item.Settings());
    public static final Item NICKEL_NUGGET = new Item(new Item.Settings());
    // platinum
    public static final Item PLATINUM_INGOT = new Item(new Item.Settings());
    public static final Item RAW_PLATINUM = new Item(new Item.Settings());
    public static final Item PLATINUM_CLUMP = new Item(new Item.Settings());
    public static final Item SMALL_PLATINUM_CLUMP = new Item(new Item.Settings());
    public static final Item PLATINUM_DUST = new Item(new Item.Settings());
    public static final Item SMALL_PLATINUM_DUST = new Item(new Item.Settings());
    public static final Item PLATINUM_GEM = new Item(new Item.Settings());
    public static final Item PLATINUM_NUGGET = new Item(new Item.Settings());
    // iron
    public static final Item IRON_CLUMP = new Item(new Item.Settings());
    public static final Item SMALL_IRON_CLUMP = new Item(new Item.Settings());
    public static final Item IRON_DUST = new Item(new Item.Settings());
    public static final Item SMALL_IRON_DUST = new Item(new Item.Settings());
    public static final Item IRON_GEM = new Item(new Item.Settings());
    // copper
    public static final Item COPPER_CLUMP = new Item(new Item.Settings());
    public static final Item SMALL_COPPER_CLUMP = new Item(new Item.Settings());
    public static final Item COPPER_DUST = new Item(new Item.Settings());
    public static final Item SMALL_COPPER_DUST = new Item(new Item.Settings());
    public static final Item COPPER_GEM = new Item(new Item.Settings());
    public static final Item COPPER_NUGGET = new Item(new Item.Settings());
    // gold
    public static final Item GOLD_CLUMP = new Item(new Item.Settings());
    public static final Item SMALL_GOLD_CLUMP = new Item(new Item.Settings());
    public static final Item GOLD_DUST = new Item(new Item.Settings());
    public static final Item SMALL_GOLD_DUST = new Item(new Item.Settings());
    public static final Item GOLD_GEM = new Item(new Item.Settings());
    // alloys
    public static final Item FLUXITE = new CustomTooltipItem(new Item.Settings(), "tooltip.oritech.fluxite");
    public static final Item ADAMANT_INGOT = new Item(new Item.Settings());
    public static final Item ADAMANT_DUST = new Item(new Item.Settings());
    public static final Item BIOSTEEL_INGOT = new Item(new Item.Settings());
    public static final Item BIOSTEEL_DUST = new Item(new Item.Settings());
    public static final Item DURATIUM_INGOT = new Item(new Item.Settings());
    public static final Item DURATIUM_DUST = new Item(new Item.Settings());
    public static final Item ELECTRUM_INGOT = new Item(new Item.Settings());
    public static final Item ELECTRUM_DUST = new Item(new Item.Settings());
    public static final Item ENERGITE_INGOT = new Item(new Item.Settings());
    public static final Item ENERGITE_DUST = new Item(new Item.Settings());
    public static final Item PROMETHEUM_INGOT = new Item(new Item.Settings());
    public static final Item STEEL_INGOT = new Item(new Item.Settings());
    public static final Item STEEL_DUST = new Item(new Item.Settings());
    //endregion
    
    // region crafting components
    public static final Item COAL_DUST = new Item(new Item.Settings());
    public static final Item CARBON_FIBRE_STRANDS = new Item(new Item.Settings());
    public static final Item ENDERIC_COMPOUND = new Item(new Item.Settings());
    public static final Item INSULATED_WIRE = new Item(new Item.Settings());
    public static final Item MAGNETIC_COIL = new Item(new Item.Settings());
    public static final Item MOTOR = new Item(new Item.Settings());
    public static final Item BASIC_BATTERY = new Item(new Item.Settings());
    public static final Item RAW_SILICON = new Item(new Item.Settings());
    public static final Item SILICON = new Item(new Item.Settings());
    public static final Item RAW_BIOPOLYMER = new Item(new Item.Settings());
    public static final Item POLYMER_RESIN = new Item(new Item.Settings());
    public static final Item PLASTIC_SHEET = new Item(new Item.Settings());
    public static final Item PROCESSING_UNIT = new Item(new Item.Settings());
    public static final Item ADVANCED_COMPUTING_ENGINE = new Item(new Item.Settings());
    public static final Item SILICON_WAFER = new Item(new Item.Settings());
    public static final Item DUBIOS_CONTAINER = new MobCaptureItem(new Item.Settings().maxCount(16), List.of(EntityType.VEX, EntityType.ALLAY, EntityType.PHANTOM));
    public static final Item ENDERIC_LENS = new Item(new Item.Settings());
    public static final Item FLUX_GATE = new Item(new Item.Settings());
    public static final Item ADVANCED_BATTERY = new Item(new Item.Settings());
    public static final Item SUPER_AI_CHIP = new Item(new Item.Settings().maxCount(4));
    public static final Item UNHOLY_INTELLIGENCE = new CustomTooltipItem(new Item.Settings().maxCount(1), "tooltip.oritech.intelligence_item");
    public static final Item HEISENBERG_COMPENSATOR = new Item(new Item.Settings().maxCount(1));
    public static final Item OVERCHARGED_CRYSTAL = new CustomTooltipItem(new Item.Settings().maxCount(1), "tooltip.oritech.overchargedcrystal");
    public static final Item SUPERCONDUCTOR = new Item(new Item.Settings());
    @Compostable(1.0F)
    public static final Item PACKED_WHEAT = new Item(new Item.Settings());
    public static final Item QUARTZ_DUST = new Item(new Item.Settings());
    
    // bio
    @Compostable(0.3F)
    public static final Item BIOMASS = new Item(new Item.Settings().food(FoodComponents.POISONOUS_POTATO));
    public static final Item SOLID_BIOFUEL = new Item(new Item.Settings());

    @Override
    public void postProcessField(String namespace, Item value, String identifier, Field field) {
        ItemRegistryContainer.super.postProcessField(namespace, value, identifier, field);

        var targetGroup = Groups.components;
        if (field.isAnnotationPresent(ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemGroupTarget.class).value();
        }
        
        if (!field.isAnnotationPresent(NoModelGeneration.class)) {
            ModelGenerator.autoRegisteredModels.add(value);
        }

        if (field.isAnnotationPresent(Compostable.class)) {
            ComposterBlock.registerCompostableItem(field.getAnnotation(Compostable.class).value(), value);
        }

        ItemGroups.add(targetGroup, value);
    }
    
    public enum Groups {
        machines, components, equipment, decorative
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
}
