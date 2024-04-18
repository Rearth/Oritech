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
