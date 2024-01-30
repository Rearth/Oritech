package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.block.base.entity.AddonBlockEntity;
import rearth.oritech.block.entity.AssemblerBlockEntity;
import rearth.oritech.block.entity.GrinderBlockEntity;
import rearth.oritech.block.entity.PulverizerBlockEntity;
import rearth.oritech.util.EnergyProvider;
import team.reborn.energy.api.EnergyStorage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class BlockEntitiesContent implements AutoRegistryContainer<BlockEntityType<?>> {

    @AssignSidedEnergy
    public static final BlockEntityType<PulverizerBlockEntity> PULVERIZER_ENTITY = FabricBlockEntityTypeBuilder.create(PulverizerBlockEntity::new, BlockContent.PULVERIZER_BLOCK).build();
    
    @AssignSidedEnergy
    public static final BlockEntityType<GrinderBlockEntity> GRINDER_ENTITY = FabricBlockEntityTypeBuilder.create(GrinderBlockEntity::new, BlockContent.GRINDER_BLOCK).build();
    
    @AssignSidedEnergy
    public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER_ENTITY = FabricBlockEntityTypeBuilder.create(AssemblerBlockEntity::new, BlockContent.ASSEMBLER_BLOCK).build();
    
    public static final BlockEntityType<AddonBlockEntity> ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(AddonBlockEntity::new,
      BlockContent.MACHINE_SPEED_ADDON,
      BlockContent.MACHINE_EFFICIENCY_ADDON,
      BlockContent.MACHINE_EXTENDER
    ).build();

    @Override
    public Registry<BlockEntityType<?>> getRegistry() {
        return Registries.BLOCK_ENTITY_TYPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<BlockEntityType<?>> getTargetFieldType() {
        return (Class<BlockEntityType<?>>) (Object) BlockEntityType.class;
    }

    @Override
    public void postProcessField(String namespace, BlockEntityType<?> value, String identifier, Field field) {
        AutoRegistryContainer.super.postProcessField(namespace, value, identifier, field);

        if (field.isAnnotationPresent(AssignSidedEnergy.class))
            EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((EnergyProvider) blockEntity).getStorage(), value);

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedEnergy {}
}
