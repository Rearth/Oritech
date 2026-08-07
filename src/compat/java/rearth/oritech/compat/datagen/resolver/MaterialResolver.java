package rearth.oritech.compat.datagen.resolver;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

public abstract class MaterialResolver<M, T> {
    protected final HolderGetter<Item> itemGetter;

    public MaterialResolver(HolderGetter<Item> itemGetter) {
        this.itemGetter = itemGetter;
    }

    // Returns unqualified item name for use in building recipe IDs
    public String name(M material, T type) {
        return this.itemKey(material, type).identifier().getPath();
    }

    // Return an ingredient using the tag for material and type
    public abstract Ingredient ingredient(M material, T type);

    // Resolve a registry item
    // Priorities are vanilla, then oritech, then ATO
    public abstract ResourceKey<Item> itemKey(M material, T type);


    public Holder<Item> holder(M material, T type) {
        var key = this.itemKey(material, type);
        return this.itemGetter.getOrThrow(key);
    }

    public Item item(M material, T type) {
        return holder(material, type).value();
    }

    public ItemStackTemplate stack(M material, T type) {
        return stack(material, type, 1);
    }

    public ItemStackTemplate stack(M material, T type, int count) {
        var itemHolder = holder(material, type);
        return new ItemStackTemplate(itemHolder, count);
    }
}
