package rearth.oritech.compat.datagen.resolver;

import net.allthemods.alltheores.common.material.Material;
import net.allthemods.alltheores.common.parts.BlockPartType;
import net.allthemods.alltheores.common.parts.ItemPartType;
import net.allthemods.alltheores.common.parts.MaterialPartType;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import rearth.oritech.Oritech;

public class ATOMaterialResolver extends MaterialResolver<Material, MaterialPartType<?,?,?>> {

    public ATOMaterialResolver(HolderGetter<Item> itemGetter) {
        super(itemGetter);
    }

    @Override
    public Ingredient ingredient(Material material, MaterialPartType<?,?,?> type) {
        var tag = itemTag(material, type);
        return Ingredient.of(this.itemGetter.getOrThrow(tag));
    }

    @Override
    public TagKey<Item> itemTag(Material material, MaterialPartType<?,?,?> type) {
        if (type instanceof ItemPartType itemType) {
            return material.get(itemType).getTag();
        } else if (type instanceof BlockPartType blockType) {
            // Create item tag using the block tag's identifier
            return TagKey.create(Registries.ITEM, material.get(blockType).getTag().location());
        } else {
            throw new IllegalArgumentException("Unexpected type " + type + ". Expected ItemPartType or BlockPartType");
        }
    }

    @Override
    public ResourceKey<Item> itemKey(Material material, MaterialPartType<?,?,?> type) {
        ResourceKey<Item> key;
        if (type instanceof ItemPartType itemType) {
            key = material.get(itemType).getHolder().getKey();
        } else if (type instanceof BlockPartType blockType) {
            key = ResourceKey.create(Registries.ITEM, material.get(blockType).getHolder().getId());
        } else {
            throw new IllegalArgumentException("Unexpected type " + type + ". Expected ItemPartType or BlockPartType");
        }
        var oritechKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Oritech.MOD_ID, key.identifier().getPath()));

        if (this.itemGetter.get(oritechKey).isPresent()) {
            return oritechKey;
        } else {
            return key;
        }        
    }
}
