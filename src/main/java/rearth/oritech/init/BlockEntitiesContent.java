package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.block.entity.PulverizerBlockEntity;

public class BlockEntitiesContent implements AutoRegistryContainer<BlockEntityType<?>> {

    public static final BlockEntityType<PulverizerBlockEntity> PULVERIZER_ENTITY = FabricBlockEntityTypeBuilder.create(PulverizerBlockEntity::new, BlockContent.PULVERIZER_BLOCK).build();

    @Override
    public Registry<BlockEntityType<?>> getRegistry() {
        return Registries.BLOCK_ENTITY_TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<BlockEntityType<?>> getTargetFieldType() {
        return (Class<BlockEntityType<?>>) (Object) BlockEntityType.class;
    }
}
