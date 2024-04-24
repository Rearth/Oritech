package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.ItemRegistryContainer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import rearth.oritech.item.tools.harvesting.EnergyPickaxeTest;
import rearth.oritech.item.tools.LaserTargetDesignator;
import rearth.oritech.item.tools.harvesting.SampleMaterial;

import java.lang.reflect.Field;

public class ItemContent implements ItemRegistryContainer {

    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Item BANANA = new Item(new FabricItemSettings());
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Item TARGET_DESIGNATOR = new LaserTargetDesignator(new FabricItemSettings().maxCount(1));
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Item TEST_ENERGY_ITEM = new EnergyPickaxeTest(4, -2.5f, new SampleMaterial());
    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Item OIL_BUCKET = new BucketItem(FluidContent.STILL_OIL, new FabricItemSettings().recipeRemainder(Items.BUCKET).maxCount(1));
    
    // region metals
    // nickel
    public static final Item NICKEL_INGOT = new Item(new FabricItemSettings());
    public static final Item RAW_NICKEL = new Item(new FabricItemSettings());
    public static final Item NICKEL_CLUMP = new Item(new FabricItemSettings());
    public static final Item SMALL_NICKEL_CLUMP = new Item(new FabricItemSettings());
    public static final Item NICKEL_DUST = new Item(new FabricItemSettings());
    public static final Item SMALL_NICKEL_DUST = new Item(new FabricItemSettings());
    public static final Item NICKEL_GEM = new Item(new FabricItemSettings());
    public static final Item NICKEL_NUGGET = new Item(new FabricItemSettings());
    // platinum
    public static final Item PLATINUM_INGOT = new Item(new FabricItemSettings());
    public static final Item RAW_PLATINUM = new Item(new FabricItemSettings());
    public static final Item PLATINUM_CLUMP = new Item(new FabricItemSettings());
    public static final Item SMALL_PLATINUM_CLUMP = new Item(new FabricItemSettings());
    public static final Item PLATINUM_DUST = new Item(new FabricItemSettings());
    public static final Item SMALL_PLATINUM_DUST = new Item(new FabricItemSettings());
    public static final Item PLATINUM_GEM = new Item(new FabricItemSettings());
    public static final Item PLATINUM_NUGGET = new Item(new FabricItemSettings());
    // iron
    public static final Item IRON_CLUMP = new Item(new FabricItemSettings());
    public static final Item SMALL_IRON_CLUMP = new Item(new FabricItemSettings());
    public static final Item IRON_DUST = new Item(new FabricItemSettings());
    public static final Item SMALL_IRON_DUST = new Item(new FabricItemSettings());
    public static final Item IRON_GEM = new Item(new FabricItemSettings());
    // copper
    public static final Item COPPER_CLUMP = new Item(new FabricItemSettings());
    public static final Item SMALL_COPPER_CLUMP = new Item(new FabricItemSettings());
    public static final Item COPPER_DUST = new Item(new FabricItemSettings());
    public static final Item SMALL_COPPER_DUST = new Item(new FabricItemSettings());
    public static final Item COPPER_GEM = new Item(new FabricItemSettings());
    public static final Item COPPER_NUGGET = new Item(new FabricItemSettings());
    // gold
    public static final Item GOLD_CLUMP = new Item(new FabricItemSettings());
    public static final Item SMALL_GOLD_CLUMP = new Item(new FabricItemSettings());
    public static final Item GOLD_DUST = new Item(new FabricItemSettings());
    public static final Item SMALL_GOLD_DUST = new Item(new FabricItemSettings());
    public static final Item GOLD_GEM = new Item(new FabricItemSettings());
    // alloys
    public static final Item FLUXITE = new Item(new FabricItemSettings());
    public static final Item ADAMANT_INGOT = new Item(new FabricItemSettings());
    public static final Item ADAMANT_DUST = new Item(new FabricItemSettings());
    public static final Item BIOSTEEL_INGOT = new Item(new FabricItemSettings());
    public static final Item BIOSTEEL_DUST = new Item(new FabricItemSettings());
    public static final Item DURATIUM_INGOT = new Item(new FabricItemSettings());
    public static final Item DURATIUM_DUST = new Item(new FabricItemSettings());
    public static final Item ELECTRUM_INGOT = new Item(new FabricItemSettings());
    public static final Item ELECTRUM_DUST = new Item(new FabricItemSettings());
    public static final Item ENERGITE_INGOT = new Item(new FabricItemSettings());
    public static final Item ENERGITE_DUST = new Item(new FabricItemSettings());
    public static final Item PROMETHEUM_INGOT = new Item(new FabricItemSettings());
    public static final Item PROMETHEUM_DUST = new Item(new FabricItemSettings());
    public static final Item STEEL_INGOT = new Item(new FabricItemSettings());
    public static final Item STEEL_DUST = new Item(new FabricItemSettings());
    //endregion

    @Override
    public void postProcessField(String namespace, Item value, String identifier, Field field) {
        ItemRegistryContainer.super.postProcessField(namespace, value, identifier, field);

        var targetGroup = ItemGroups.GROUPS.first;
        if (field.isAnnotationPresent(ItemGroups.ItemGroupTarget.class)) {
            targetGroup = field.getAnnotation(ItemGroups.ItemGroupTarget.class).value();
        }

        ItemGroups.add(targetGroup, value);
    }
}
