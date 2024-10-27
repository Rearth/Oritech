package rearth.oritech.init;

import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.block.entity.decorative.TechDoorBlockEntity;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorMotorBlockEntity;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorSensorBlockEntity;
import rearth.oritech.block.entity.machines.accelerator.BlackHoleBlockEntity;
import rearth.oritech.block.entity.machines.addons.*;
import rearth.oritech.block.entity.machines.generators.*;
import rearth.oritech.block.entity.machines.interaction.*;
import rearth.oritech.block.entity.machines.processing.*;
import rearth.oritech.block.entity.machines.storage.*;
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
    public static final BlockEntityType<FragmentForgeBlockEntity> FRAGMENT_FORGE_ENTITY = FabricBlockEntityTypeBuilder.create(FragmentForgeBlockEntity::new, BlockContent.FRAGMENT_FORGE_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER_ENTITY = FabricBlockEntityTypeBuilder.create(AssemblerBlockEntity::new, BlockContent.ASSEMBLER_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<FoundryBlockEntity> FOUNDRY_ENTITY = FabricBlockEntityTypeBuilder.create(FoundryBlockEntity::new, BlockContent.FOUNDRY_BLOCK).build();
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<CentrifugeBlockEntity> CENTRIFUGE_ENTITY = FabricBlockEntityTypeBuilder.create(CentrifugeBlockEntity::new, BlockContent.CENTRIFUGE_BLOCK).build();
    
    @AssignSidedInventory
    public static final BlockEntityType<AtomicForgeBlockEntity> ATOMIC_FORGE_ENTITY = FabricBlockEntityTypeBuilder.create(AtomicForgeBlockEntity::new, BlockContent.ATOMIC_FORGE_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<BioGeneratorEntity> BIO_GENERATOR_ENTITY = FabricBlockEntityTypeBuilder.create(BioGeneratorEntity::new, BlockContent.BIO_GENERATOR_BLOCK).build();
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<LavaGeneratorEntity> LAVA_GENERATOR_ENTITY = FabricBlockEntityTypeBuilder.create(LavaGeneratorEntity::new, BlockContent.LAVA_GENERATOR_BLOCK).build();
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<FuelGeneratorEntity> FUEL_GENERATOR_ENTITY = FabricBlockEntityTypeBuilder.create(FuelGeneratorEntity::new, BlockContent.FUEL_GENERATOR_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<BasicGeneratorEntity> BASIC_GENERATOR_ENTITY = FabricBlockEntityTypeBuilder.create(BasicGeneratorEntity::new, BlockContent.BASIC_GENERATOR_BLOCK).build();
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<SteamEngineEntity> STEAM_ENGINE_ENTITY = FabricBlockEntityTypeBuilder.create(SteamEngineEntity::new, BlockContent.STEAM_ENGINE_BLOCK).build();
    
    @AssignSidedEnergy
    public static final BlockEntityType<BigSolarPanelEntity> BIG_SOLAR_ENTITY = FabricBlockEntityTypeBuilder.create(BigSolarPanelEntity::new, BlockContent.BIG_SOLAR_PANEL_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PoweredFurnaceBlockEntity> POWERED_FURNACE_ENTITY = FabricBlockEntityTypeBuilder.create(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<LaserArmBlockEntity> LASER_ARM_ENTITY = FabricBlockEntityTypeBuilder.create(LaserArmBlockEntity::new, BlockContent.LASER_ARM_BLOCK).build();
    
    @AssignSidedInventory
    public static final BlockEntityType<DeepDrillEntity> DEEP_DRILL_ENTITY = FabricBlockEntityTypeBuilder.create(DeepDrillEntity::new, BlockContent.DEEP_DRILL_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<DronePortEntity> DRONE_PORT_ENTITY = FabricBlockEntityTypeBuilder.create(DronePortEntity::new, BlockContent.DRONE_PORT_BLOCK).build();
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<PumpBlockEntity> PUMP_BLOCK = FabricBlockEntityTypeBuilder.create(PumpBlockEntity::new, BlockContent.PUMP_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<ChargerBlockEntity> CHARGER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(ChargerBlockEntity::new, BlockContent.CHARGER_BLOCK).build();
    
    @AssignSidedEnergy
    public static final BlockEntityType<EnergyAcceptorAddonBlockEntity> ENERGY_ACCEPTOR_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(EnergyAcceptorAddonBlockEntity::new, BlockContent.MACHINE_ACCEPTOR_ADDON).build();

    public static final BlockEntityType<RedstoneAddonBlockEntity> REDSTONE_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(RedstoneAddonBlockEntity::new, BlockContent.MACHINE_REDSTONE_ADDON).build();
    
    @AssignSidedFluid
    public static final BlockEntityType<SteamBoilerAddonBlockEntity> STEAM_BOILER_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(SteamBoilerAddonBlockEntity::new, BlockContent.STEAM_BOILER_ADDON).build();
    
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
    
    @AssignSidedEnergy
    @AssignSidedInventory
    public static final BlockEntityType<TreefellerBlockEntity> TREEFELLER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(TreefellerBlockEntity::new, BlockContent.TREEFELLER_BLOCK).build();
    
    @AssignSidedInventory
    public static final BlockEntityType<EnchantmentCatalystBlockEntity> ENCHANTMENT_CATALYST_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(EnchantmentCatalystBlockEntity::new, BlockContent.ENCHANTMENT_CATALYST_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<EnchanterBlockEntity> ENCHANTER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(EnchanterBlockEntity::new, BlockContent.ENCHANTER_BLOCK).build();
    
    public static final BlockEntityType<SpawnerControllerBlockEntity> SPAWNER_CONTROLLER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(SpawnerControllerBlockEntity::new, BlockContent.SPAWNER_CONTROLLER_BLOCK).build();
    
    @AssignSidedInventory
    public static final BlockEntityType<AcceleratorControllerBlockEntity> ACCELERATOR_CONTROLLER_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(AcceleratorControllerBlockEntity::new, BlockContent.ACCELERATOR_CONTROLLER).build();
    public static final BlockEntityType<AcceleratorSensorBlockEntity> ACCELERATOR_SENSOR_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(AcceleratorSensorBlockEntity::new, BlockContent.ACCELERATOR_SENSOR).build();
    @AssignSidedEnergy
    public static final BlockEntityType<AcceleratorMotorBlockEntity> ACCELERATOR_MOTOR_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(AcceleratorMotorBlockEntity::new, BlockContent.ACCELERATOR_MOTOR).build();
    public static final BlockEntityType<BlackHoleBlockEntity> BLACK_HOLE_ENTITY = FabricBlockEntityTypeBuilder.create(BlackHoleBlockEntity::new, BlockContent.BLACK_HOLE_BLOCK).build();
    
    public static final BlockEntityType<InventoryProxyAddonBlockEntity> INVENTORY_PROXY_ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<SmallStorageBlockEntity> SMALL_STORAGE_ENTITY = FabricBlockEntityTypeBuilder.create(SmallStorageBlockEntity::new, BlockContent.SMALL_STORAGE_BLOCK).build();
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<LargeStorageBlockEntity> LARGE_STORAGE_ENTITY = FabricBlockEntityTypeBuilder.create(LargeStorageBlockEntity::new, BlockContent.LARGE_STORAGE_BLOCK).build();
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<CreativeStorageBlockEntity> CREATIVE_STORAGE_ENTITY = FabricBlockEntityTypeBuilder.create(CreativeStorageBlockEntity::new, BlockContent.CREATIVE_STORAGE_BLOCK).build();
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final BlockEntityType<SmallFluidTankEntity> SMALL_TANK_ENTITY = FabricBlockEntityTypeBuilder.create((pos, state) -> new SmallFluidTankEntity(pos, state, false), BlockContent.SMALL_TANK_BLOCK).build();

    @AssignSidedInventory
    @AssignSidedFluid
    public static final BlockEntityType<SmallFluidTankEntity> CREATIVE_TANK_ENTITY = FabricBlockEntityTypeBuilder.create((pos, state) -> new SmallFluidTankEntity(pos, state, true), BlockContent.CREATIVE_TANK_BLOCK).build();
    
    @AssignSidedFluid
    public static final BlockEntityType<FluidPipeInterfaceEntity> FLUID_PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(FluidPipeInterfaceEntity::new, BlockContent.FLUID_PIPE_CONNECTION).build();
    @AssignSidedEnergy
    public static final BlockEntityType<EnergyPipeInterfaceEntity> ENERGY_PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(EnergyPipeInterfaceEntity::new, BlockContent.ENERGY_PIPE_CONNECTION, BlockContent.SUPERCONDUCTOR_CONNECTION).build();
    public static final BlockEntityType<ItemPipeInterfaceEntity> ITEM_PIPE_ENTITY = FabricBlockEntityTypeBuilder.create(ItemPipeInterfaceEntity::new, BlockContent.ITEM_PIPE_CONNECTION).build();
    @AssignSidedInventory
    public static final BlockEntityType<ItemFilterBlockEntity> ITEM_FILTER_ENTITY = FabricBlockEntityTypeBuilder.create(ItemFilterBlockEntity::new, BlockContent.ITEM_FILTER_BLOCK).build();
    
    
    public static final BlockEntityType<AddonBlockEntity> ADDON_ENTITY = FabricBlockEntityTypeBuilder.create(AddonBlockEntity::new,
      BlockContent.MACHINE_SPEED_ADDON,
      BlockContent.MACHINE_EFFICIENCY_ADDON,
      BlockContent.MACHINE_FLUID_ADDON,
      BlockContent.MACHINE_HUNTER_ADDON,
      BlockContent.MACHINE_YIELD_ADDON,
      BlockContent.CROP_FILTER_ADDON,
      BlockContent.MACHINE_EXTENDER,
      BlockContent.MACHINE_CAPACITOR_ADDON,
      BlockContent.CAPACITOR_ADDON_EXTENDER,
      BlockContent.QUARRY_ADDON,
      BlockContent.MACHINE_HUNTER_ADDON,
      BlockContent.QUARRY_ADDON
    ).build();
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final BlockEntityType<MachineCoreEntity> MACHINE_CORE_ENTITY = FabricBlockEntityTypeBuilder.create(MachineCoreEntity::new,
      BlockContent.MACHINE_CORE_1,
      BlockContent.MACHINE_CORE_2,
      BlockContent.MACHINE_CORE_3,
      BlockContent.MACHINE_CORE_4,
      BlockContent.MACHINE_CORE_5,
      BlockContent.MACHINE_CORE_6,
      BlockContent.MACHINE_CORE_7
    ).build();
    
    public static final BlockEntityType<TechDoorBlockEntity> TECH_DOOR_ENTITY = FabricBlockEntityTypeBuilder.create(TechDoorBlockEntity::new, BlockContent.TECH_DOOR).build();
    
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
            EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((EnergyProvider) blockEntity).getStorage(direction), value);
        
        if (field.isAnnotationPresent(AssignSidedFluid.class))
            FluidStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((FluidProvider) blockEntity).getFluidStorage(direction), value);
        
        if (field.isAnnotationPresent(AssignSidedInventory.class))
            ItemStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> ((InventoryProvider) blockEntity).getInventory(direction), value);
        
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedEnergy {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedInventory {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    public @interface AssignSidedFluid {
    }
}
