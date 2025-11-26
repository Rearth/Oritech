package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.wispforest.owo.registration.reflect.BlockRegistryContainer;
import rearth.oritech.Oritech;

import java.lang.reflect.Field;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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
    default void postProcessField(String namespace, Block value, String identifier, Field field, RegistrySupplier<Block> supplier) {
        if (field.isAnnotationPresent(BlockRegistryContainer.NoBlockItem.class)) return;
        ITEM_REGISTRY.register(identifier, () -> createBlockItem(value, identifier));
    }
    
    default BlockItem createBlockItem(Block block, String identifier) {
        return new BlockItem(block, new Item.Properties());
    }
    
    static void finishItemRegister() {
        ITEM_REGISTRY.register();
    }
    
}
