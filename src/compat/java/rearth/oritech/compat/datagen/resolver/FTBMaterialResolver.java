package rearth.oritech.compat.datagen.resolver;

import java.util.List;

import dev.ftb.mods.ftbmaterials.resources.Resource;
import dev.ftb.mods.ftbmaterials.resources.ResourceRegistries;
import dev.ftb.mods.ftbmaterials.resources.ResourceType;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

public class FTBMaterialResolver extends MaterialResolver<Resource, ResourceType> {

    public FTBMaterialResolver(HolderGetter<Item> itemGetter) {
        super(itemGetter);
    }

    @Override
    public Ingredient ingredient(Resource resource, ResourceType type) {
        var tag = tag(resource, type);
        return Ingredient.of(this.itemGetter.getOrThrow(tag));
    }

    protected TagKey<Item> tag(Resource resource, ResourceType type) {
        var resourceName = resource.name().toLowerCase();
        // first tag in "c" namespace. Looking at the ResourceType class, all resource types have a "c" tag,
        // and some also have a "mekanism" tag which can be safely ignored here
        var cTag = Identifier.parse(type.getTags()
            .stream()
            .filter(tag -> tag.startsWith("c:"))
            .findFirst()
            .get());

        return ItemTags.create(cTag.withSuffix("/" + resourceName));
    }

    private String resourcePath(Resource resource, ResourceType type) {
        var resourceName = resource.name().toLowerCase();
        return switch (type) {
            case ResourceType.RAW_ORE -> "raw_" + resourceName;
            default -> resourceName + "_" + type.name().toLowerCase();
        };
    }

    @Override
    public ResourceKey<Item> itemKey(Resource resource, ResourceType type) {
        var itemName = resourcePath(resource, type);

        // Give preference to minecraft and oritech items
        for (var namespace : List.of("minecraft", "oritech")) {
            // Create a key for a potential item in the namespace
            var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, itemName));
            var itemHolder = this.itemGetter.get(key);

            // if the item exists, use that key
            if (itemHolder.isPresent()) return key;
        }
        // No minecraft or oritech item with the same name, use the FTB Materials item key
        return ResourceRegistries.getItemOrThrow(resource, type).getKey();
    }
}
