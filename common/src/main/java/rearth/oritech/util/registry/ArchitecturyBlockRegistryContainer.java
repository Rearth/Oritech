package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;

import java.lang.reflect.Field;

public interface ArchitecturyBlockRegistryContainer extends ArchitecturyRegistryContainer<Block> {
    
    DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(Oritech.MOD_ID, Registries.ITEM);
    
    @Override
    default ResourceKey<Registry<Block>> getRegistryType() {
        return Registries.BLOCK;
    }
    
    @Override
    default Class<Block> getTargetFieldType() {
        return Block.class;
    }
    
    @Override
    void postProcessField(String namespace, Block value, String identifier, Field field, RegistrySupplier<Block> supplier);
    
    default BlockItem createBlockItem(Block block, @Nullable Rarity rarity, String identifier) {
        var properties = new Item.Properties();
        if (rarity != null) {
            properties = properties.rarity(rarity);
        }
        
        
        return new BlockItem(block, properties);
    }
    
    static void finishItemRegister() {
        ITEM_REGISTRY.register();
    }
    
}
