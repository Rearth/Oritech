package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.ItemRegistryContainer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;

import java.lang.reflect.Field;

public class ItemContent implements ItemRegistryContainer {

    @ItemGroups.ItemGroupTarget(ItemGroups.GROUPS.second)
    public static final Item BANANA = new Item(new FabricItemSettings());

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
