package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import rearth.oritech.Oritech;

import java.lang.reflect.Field;

public interface ArchitecturyBlockRegistryContainer extends ArchitecturyRegistryContainer<Block> {
    
    DeferredRegister<Item> ITEM_REGISTRY = DeferredRegister.create(Oritech.MOD_ID, RegistryKeys.ITEM);
    
    @Override
    default RegistryKey<Registry<Block>> getRegistryType() {
        return RegistryKeys.BLOCK;
    }
    
    @Override
    default Class<Block> getTargetFieldType() {
        return Block.class;
    }
    
    @Override
    default void postProcessField(String namespace, Block value, String identifier, Field field, RegistrySupplier<Block> supplier) {
        if (field.isAnnotationPresent(BlockRegistryContainer.NoBlockItem.class)) return;
        ITEM_REGISTRY.register(identifier, () -> createBlockItem(value, identifier));
    }
    
    default BlockItem createBlockItem(Block block, String identifier) {
        return new BlockItem(block, new Item.Settings());
    }
    
    static void finishItemRegister() {
        ITEM_REGISTRY.register();
    }
    
}
