package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.addons.AddonBlockEntity;
import rearth.oritech.block.entity.machines.addons.EnergyAcceptorAddonBlockEntity;
import rearth.oritech.block.entity.machines.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.machines.generators.BasicGeneratorEntity;
import rearth.oritech.block.entity.machines.generators.BigSolarPanelEntity;
import rearth.oritech.block.entity.machines.generators.TestGeneratorEntity;
import rearth.oritech.block.entity.machines.interaction.*;
import rearth.oritech.block.entity.machines.processing.*;
import rearth.oritech.block.entity.pipes.EnergyPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.FluidPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.FluidProvider;
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
    public static final BlockEntityType<FoundryBlockEntity> FOUNDRY_ENTITY = FabricBlockEntityTypeBuilder.create(FoundryBlockEntity::new, BlockContent.FOUNDRY_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<CentrifugeBlockEntity> CENTRIFUGE_ENTITY = FabricBlockEntityTypeBuilder.create(CentrifugeBlockEntity::new, BlockContent.CENTRIFUGE_BLOCK).build();
    
    @AssignSidedInventory
    public static final BlockEntityType<AtomicForgeBlockEntity> ATOMIC_FORGE_ENTITY = FabricBlockEntityTypeBuilder.create(AtomicForgeBlockEntity::new, BlockContent.ATOMIC_FORGE_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<TestGeneratorEntity> TEST_GENERATOR_ENTITY = FabricBlockEntityTypeBuilder.create(TestGeneratorEntity::new, BlockContent.TEST_GENERATOR_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<BasicGeneratorEntity> BASIC_GENERATOR_ENTITY = FabricBlockEntityTypeBuilder.create(BasicGeneratorEntity::new, BlockContent.BASIC_GENERATOR_BLOCK).build();
    
    @AssignSidedEnergy
    public static final BlockEntityType<BigSolarPanelEntity> BIG_SOLAR_ENTITY = FabricBlockEntityTypeBuilder.create(BigSolarPanelEntity::new, BlockContent.BIG_SOLAR_PANEL_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PoweredFurnaceBlockEntity> POWERED_FURNACE_ENTITY = FabricBlockEntityTypeBuilder.create(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<LaserArmBlockEntity> LASER_ARM_BLOCK = FabricBlockEntityTypeBuilder.create(LaserArmBlockEntity::new, BlockContent.LASER_ARM_BLOCK).build();
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<PumpBlockEntity> PUMP_BLOCK = FabricBlockEntityTypeBuilder.create(PumpBlockEntity::new, BlockContent.PUMP_BLOCK).build();
    
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
    @AssignSidedFluid
    public static final BlockEntityType<FertilizerBlockEntity> FERTILIZER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(FertilizerBlockEntity::new, BlockContent.FERTILIZER_BLOCK).build();
    
    public static final BlockEntityType<InventoryProxyAddonBlockEntity> INVENTORY_PROXY_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON).build();
    
    @AssignSidedFluid
    public static final BlockEntityType<FluidPipeInterfaceEntity> FLUID_PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(FluidPipeInterfaceEntity::new, BlockContent.FLUID_PIPE_CONNECTION).build();
    @AssignSidedEnergy
    public static final BlockEntityType<EnergyPipeInterfaceEntity> ENERGY_PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(EnergyPipeInterfaceEntity::new, BlockContent.ENERGY_PIPE_CONNECTION).build();
    public static final BlockEntityType<ItemPipeInterfaceEntity> ITEM_PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(ItemPipeInterfaceEntity::new, BlockContent.ITEM_PIPE_CONNECTION).build();
    @AssignSidedInventory
    public static final BlockEntityType<ItemFilterBlockEntity> ITEM_FILTER_ENTITY = FabricBlockEntityTypeBuilder.create(ItemFilterBlockEntity::new, BlockContent.ITEM_FILTER_BLOCK).build();
    
    
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
        
        if (field.isAnnotationPresent(AssignSidedFluid.class))
            FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((FluidProvider) blockEntity).getFluidStorage(direction), value);
        
        if (field.isAnnotationPresent(AssignSidedInventory.class))
            ItemStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((InventoryProvider) blockEntity).getInventory(direction), value);

    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedEnergy {}
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedInventory {}
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedFluid {}
}
