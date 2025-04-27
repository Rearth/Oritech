package rearth.oritech.util;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;

public class TagUtils {
    public static String C_TAG_NAMESPACE = "c";

    public static TagKey<Item> getStorageBlockTag(String path) {
        return cItemTag("storage_blocks/" + path);
    }
    
    public static TagKey<Item> getIngotTag(String path) {
        return cItemTag("ingots/" + path);
    }

    public static TagKey<Item> getClumpTag(String path) {
        return cItemTag("clumps/" + path);
    }
    
    public static TagKey<Item> getDustTag(String path) {
        return cItemTag("dusts/" + path);
    }

    public static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(namespace, path));
    }

    public static TagKey<Item> cItemTag(String path) {
        return itemTag(C_TAG_NAMESPACE, path);
    }

    public static TagKey<Block> cBlockTag(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.of(C_TAG_NAMESPACE, path));
    }

    public static TagKey<Item> oritechItemTag(String path) {
        return TagKey.of(RegistryKeys.ITEM, Oritech.id(path));
    }

    public static TagKey<Block> oritechBlockTag(String path) {
        return TagKey.of(RegistryKeys.BLOCK, Oritech.id(path));
    }

    public static TagKey<Fluid> cFluidTag(String path) {
        return fluidTag(C_TAG_NAMESPACE, path);
    }

    public static TagKey<Fluid> fluidTag(String namespace, String path) {        
        return TagKey.of(RegistryKeys.FLUID, Identifier.of(namespace, path));
    }

    public static TagKey<Fluid> oritechFluidTag(String path) {
        return fluidTag(Oritech.MOD_ID, path);
    }
}
