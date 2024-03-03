package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.block.entity.machines.addons.EnergyAcceptorAddonBlockEntity;
import rearth.oritech.block.entity.machines.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.machines.interaction.DestroyerBlockEntity;
import rearth.oritech.block.entity.machines.interaction.FertilizerBlockEntity;
import rearth.oritech.block.entity.machines.interaction.PlacerBlockEntity;
import rearth.oritech.block.entity.machines.processing.*;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.InventoryProvider;
import team.reborn.energy.api.EnergyStorage;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class BlockEntitiesContent implements AutoRegistryContainer<BlockEntityType<?>> {
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PulverizerBlockEntity> PULVERIZER_ENTITY = FabricBlockEntityTypeBuilder.create(PulverizerBlockEntity::new, BlockContent.PULVERIZER_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<GrinderBlockEntity> GRINDER_ENTITY = FabricBlockEntityTypeBuilder.create(GrinderBlockEntity::new, BlockContent.GRINDER_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER_ENTITY = FabricBlockEntityTypeBuilder.create(AssemblerBlockEntity::new, BlockContent.ASSEMBLER_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PoweredFurnaceBlockEntity> POWERED_FURNACE_ENTITY = FabricBlockEntityTypeBuilder.create(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE_BLOCK).build();
    
    @AssignSidedEnergy
    public static final BlockEntityType<EnergyAcceptorAddonBlockEntity> ENERGY_ACCEPTOR_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(EnergyAcceptorAddonBlockEntity::new, BlockContent.MACHINE_ACCEPTOR_ADDON).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PlacerBlockEntity> PLACER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(PlacerBlockEntity::new, BlockContent.PLACER_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<DestroyerBlockEntity> DESTROYER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(DestroyerBlockEntity::new, BlockContent.DESTROYER_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<FertilizerBlockEntity> FERTILIZER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(FertilizerBlockEntity::new, BlockContent.FERTILIZER_BLOCK).build();
    
    public static final BlockEntityType<InventoryProxyAddonBlockEntity> INVENTORY_PROXY_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON).build();
    
    public static final BlockEntityType<AddonBlockEntity> ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(AddonBlockEntity::new,
      BlockContent.MACHINE_SPEED_ADDON,
      BlockContent.MACHINE_EFFICIENCY_ADDON,
      BlockContent.MACHINE_EXTENDER,
      BlockContent.MACHINE_CAPACITOR_ADDON
    ).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<MachineCoreEntity> MACHINE_CORE_ENTITY = FabricBlockEntityTypeBuilder.create(MachineCoreEntity::new,
      BlockContent.MACHINE_CORE_BASIC,
      BlockContent.MACHINE_CORE_GOOD
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
        
        if (field.isAnnotationPresent(AssignSidedInventory.class))
            ItemStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((InventoryProvider) blockEntity).getInventory(direction), value);

    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedEnergy {}
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedInventory {}
}
