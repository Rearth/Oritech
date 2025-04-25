package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.block.entity.accelerator.*;
import rearth.oritech.block.entity.addons.*;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.block.entity.augmenter.AugmentResearchStationBlockEntity;
import rearth.oritech.block.entity.decorative.TechDoorBlockEntity;
import rearth.oritech.block.entity.generators.*;
import rearth.oritech.block.entity.interaction.*;
import rearth.oritech.block.entity.pipes.EnergyPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.FluidPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.block.entity.processing.*;
import rearth.oritech.block.entity.reactor.*;
import rearth.oritech.block.entity.storage.*;
import rearth.oritech.util.registry.ArchitecturyRegistryContainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@SuppressWarnings("deprecation")
public class BlockEntitiesContent implements ArchitecturyRegistryContainer<BlockEntityType<?>> {
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PulverizerBlockEntity> PULVERIZER_ENTITY = BlockEntityType.Builder.create(PulverizerBlockEntity::new, BlockContent.PULVERIZER_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<FragmentForgeBlockEntity> FRAGMENT_FORGE_ENTITY = BlockEntityType.Builder.create(FragmentForgeBlockEntity::new, BlockContent.FRAGMENT_FORGE_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<AssemblerBlockEntity> ASSEMBLER_ENTITY = BlockEntityType.Builder.create(AssemblerBlockEntity::new, BlockContent.ASSEMBLER_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<FoundryBlockEntity> FOUNDRY_ENTITY = BlockEntityType.Builder.create(FoundryBlockEntity::new, BlockContent.FOUNDRY_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final BlockEntityType<CoolerBlockEntity> COOLER_ENTITY = BlockEntityType.Builder.create(CoolerBlockEntity::new, BlockContent.COOLER_BLOCK).build(null);
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<CentrifugeBlockEntity> CENTRIFUGE_ENTITY = BlockEntityType.Builder.create(CentrifugeBlockEntity::new, BlockContent.CENTRIFUGE_BLOCK).build(null);
    
    @AssignSidedInventory
    public static final BlockEntityType<AtomicForgeBlockEntity> ATOMIC_FORGE_ENTITY = BlockEntityType.Builder.create(AtomicForgeBlockEntity::new, BlockContent.ATOMIC_FORGE_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<BioGeneratorEntity> BIO_GENERATOR_ENTITY = BlockEntityType.Builder.create(BioGeneratorEntity::new, BlockContent.BIO_GENERATOR_BLOCK).build(null);
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<LavaGeneratorEntity> LAVA_GENERATOR_ENTITY = BlockEntityType.Builder.create(LavaGeneratorEntity::new, BlockContent.LAVA_GENERATOR_BLOCK).build(null);
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<FuelGeneratorEntity> FUEL_GENERATOR_ENTITY = BlockEntityType.Builder.create(FuelGeneratorEntity::new, BlockContent.FUEL_GENERATOR_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<BasicGeneratorEntity> BASIC_GENERATOR_ENTITY = BlockEntityType.Builder.create(BasicGeneratorEntity::new, BlockContent.BASIC_GENERATOR_BLOCK).build(null);
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<SteamEngineEntity> STEAM_ENGINE_ENTITY = BlockEntityType.Builder.create(SteamEngineEntity::new, BlockContent.STEAM_ENGINE_BLOCK).build(null);
    
    @AssignSidedEnergy
    public static final BlockEntityType<BigSolarPanelEntity> BIG_SOLAR_ENTITY = BlockEntityType.Builder.create(BigSolarPanelEntity::new, BlockContent.BIG_SOLAR_PANEL_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PoweredFurnaceBlockEntity> POWERED_FURNACE_ENTITY = BlockEntityType.Builder.create(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<LaserArmBlockEntity> LASER_ARM_ENTITY = BlockEntityType.Builder.create(LaserArmBlockEntity::new, BlockContent.LASER_ARM_BLOCK).build(null);
    
    @AssignSidedInventory
    public static final BlockEntityType<DeepDrillEntity> DEEP_DRILL_ENTITY = BlockEntityType.Builder.create(DeepDrillEntity::new, BlockContent.DEEP_DRILL_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<DronePortEntity> DRONE_PORT_ENTITY = BlockEntityType.Builder.create(DronePortEntity::new, BlockContent.DRONE_PORT_BLOCK).build(null);
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<PumpBlockEntity> PUMP_BLOCK = BlockEntityType.Builder.create(PumpBlockEntity::new, BlockContent.PUMP_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final BlockEntityType<ChargerBlockEntity> CHARGER_BLOCK_ENTITY = BlockEntityType.Builder.create(ChargerBlockEntity::new, BlockContent.CHARGER_BLOCK).build(null);
    
    @AssignSidedEnergy
    public static final BlockEntityType<EnergyAcceptorAddonBlockEntity> ENERGY_ACCEPTOR_ADDON_ENTITY = BlockEntityType.Builder.create(EnergyAcceptorAddonBlockEntity::new, BlockContent.MACHINE_ACCEPTOR_ADDON).build(null);
    
    public static final BlockEntityType<RedstoneAddonBlockEntity> REDSTONE_ADDON_ENTITY = BlockEntityType.Builder.create(RedstoneAddonBlockEntity::new, BlockContent.MACHINE_REDSTONE_ADDON).build(null);
    
    @AssignSidedFluid
    public static final BlockEntityType<SteamBoilerAddonBlockEntity> STEAM_BOILER_ADDON_ENTITY = BlockEntityType.Builder.create(SteamBoilerAddonBlockEntity::new, BlockContent.STEAM_BOILER_ADDON).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<PlacerBlockEntity> PLACER_BLOCK_ENTITY = BlockEntityType.Builder.create(PlacerBlockEntity::new, BlockContent.PLACER_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<DestroyerBlockEntity> DESTROYER_BLOCK_ENTITY = BlockEntityType.Builder.create(DestroyerBlockEntity::new, BlockContent.DESTROYER_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final BlockEntityType<FertilizerBlockEntity> FERTILIZER_BLOCK_ENTITY = BlockEntityType.Builder.create(FertilizerBlockEntity::new, BlockContent.FERTILIZER_BLOCK).build(null);
    
    @AssignSidedEnergy
    @AssignSidedInventory
    public static final BlockEntityType<TreefellerBlockEntity> TREEFELLER_BLOCK_ENTITY = BlockEntityType.Builder.create(TreefellerBlockEntity::new, BlockContent.TREEFELLER_BLOCK).build(null);
    
    @AssignSidedEnergy
    public static final BlockEntityType<PipeBoosterBlockEntity> PIPE_BOOSTER_BLOCK_ENTITY = BlockEntityType.Builder.create(PipeBoosterBlockEntity::new, BlockContent.PIPE_BOOSTER_BLOCK).build(null);
    
    @AssignSidedInventory
    public static final BlockEntityType<EnchantmentCatalystBlockEntity> ENCHANTMENT_CATALYST_BLOCK_ENTITY = BlockEntityType.Builder.create(EnchantmentCatalystBlockEntity::new, BlockContent.ENCHANTMENT_CATALYST_BLOCK).build(null);
    
    @AssignSidedEnergy
    public static final BlockEntityType<UnstableContainerBlockEntity> UNSTABLE_CONTAINER_BLOCK_ENTITY = BlockEntityType.Builder.create(UnstableContainerBlockEntity::new, BlockContent.UNSTABLE_CONTAINER).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<EnchanterBlockEntity> ENCHANTER_BLOCK_ENTITY = BlockEntityType.Builder.create(EnchanterBlockEntity::new, BlockContent.ENCHANTER_BLOCK).build(null);
    
    public static final BlockEntityType<SpawnerControllerBlockEntity> SPAWNER_CONTROLLER_BLOCK_ENTITY = BlockEntityType.Builder.create(SpawnerControllerBlockEntity::new, BlockContent.SPAWNER_CONTROLLER_BLOCK).build(null);
    
    public static final BlockEntityType<ReactorControllerBlockEntity> REACTOR_CONTROLLER_BLOCK_ENTITY = BlockEntityType.Builder.create(ReactorControllerBlockEntity::new, BlockContent.REACTOR_CONTROLLER).build(null);
    @AssignSidedInventory
    public static final BlockEntityType<ReactorFuelPortEntity> REACTOR_FUEL_PORT_BLOCK_ENTITY = BlockEntityType.Builder.create(ReactorFuelPortEntity::new, BlockContent.REACTOR_FUEL_PORT).build(null);
    @AssignSidedInventory
    public static final BlockEntityType<ReactorAbsorberPortEntity> REACTOR_ABSORBER_PORT_BLOCK_ENTITY = BlockEntityType.Builder.create(ReactorAbsorberPortEntity::new, BlockContent.REACTOR_ABSORBER_PORT).build(null);
    @AssignSidedEnergy
    public static final BlockEntityType<ReactorEnergyPortEntity> REACTOR_ENERGY_PORT_BLOCK_ENTITY = BlockEntityType.Builder.create(ReactorEnergyPortEntity::new, BlockContent.REACTOR_ENERGY_PORT).build(null);
    public static final BlockEntityType<NuclearExplosionEntity> REACTOR_EXPLOSION_ENTITY = BlockEntityType.Builder.create(NuclearExplosionEntity::new, BlockContent.REACTOR_EXPLOSION_SMALL, BlockContent.REACTOR_EXPLOSION_MEDIUM, BlockContent.REACTOR_EXPLOSION_LARGE).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<AugmentApplicationEntity> PLAYER_MODIFIER_BLOCK_ENTITY = BlockEntityType.Builder.create(AugmentApplicationEntity::new, BlockContent.AUGMENT_APPLICATION_BLOCK).build(null);
    public static final BlockEntityType<AugmentResearchStationBlockEntity> AUGMENTER_RESEARCH_STATION_ENTITY = BlockEntityType.Builder.create(AugmentResearchStationBlockEntity::new, BlockContent.SIMPLE_AUGMENT_STATION, BlockContent.ADVANCED_AUGMENT_STATION, BlockContent.ARCANE_AUGMENT_STATION).build(null);
    
    
    @AssignSidedInventory
    public static final BlockEntityType<AcceleratorControllerBlockEntity> ACCELERATOR_CONTROLLER_BLOCK_ENTITY = BlockEntityType.Builder.create(AcceleratorControllerBlockEntity::new, BlockContent.ACCELERATOR_CONTROLLER).build(null);
    public static final BlockEntityType<AcceleratorSensorBlockEntity> ACCELERATOR_SENSOR_BLOCK_ENTITY = BlockEntityType.Builder.create(AcceleratorSensorBlockEntity::new, BlockContent.ACCELERATOR_SENSOR).build(null);
    @AssignSidedEnergy
    public static final BlockEntityType<AcceleratorMotorBlockEntity> ACCELERATOR_MOTOR_BLOCK_ENTITY = BlockEntityType.Builder.create(AcceleratorMotorBlockEntity::new, BlockContent.ACCELERATOR_MOTOR).build(null);
    public static final BlockEntityType<BlackHoleBlockEntity> BLACK_HOLE_ENTITY = BlockEntityType.Builder.create(BlackHoleBlockEntity::new, BlockContent.BLACK_HOLE_BLOCK).build(null);
    @AssignSidedEnergy
    public static final BlockEntityType<ParticleCollectorBlockEntity> PARTICLE_COLLECTOR_BLOCK_ENTITY = BlockEntityType.Builder.create(ParticleCollectorBlockEntity::new, BlockContent.PARTICLE_COLLECTOR_BLOCK).build(null);
    
    @AssignSidedInventory
    public static final BlockEntityType<InventoryProxyAddonBlockEntity> INVENTORY_PROXY_ADDON_ENTITY = BlockEntityType.Builder.create(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<SmallStorageBlockEntity> SMALL_STORAGE_ENTITY = BlockEntityType.Builder.create(SmallStorageBlockEntity::new, BlockContent.SMALL_STORAGE_BLOCK).build(null);
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<LargeStorageBlockEntity> LARGE_STORAGE_ENTITY = BlockEntityType.Builder.create(LargeStorageBlockEntity::new, BlockContent.LARGE_STORAGE_BLOCK).build(null);
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final BlockEntityType<CreativeStorageBlockEntity> CREATIVE_STORAGE_ENTITY = BlockEntityType.Builder.create(CreativeStorageBlockEntity::new, BlockContent.CREATIVE_STORAGE_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final BlockEntityType<SmallTankEntity> SMALL_TANK_ENTITY = BlockEntityType.Builder.create((pos, state) -> new SmallTankEntity(pos, state, false), BlockContent.SMALL_TANK_BLOCK).build(null);
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final BlockEntityType<SmallTankEntity> CREATIVE_TANK_ENTITY = BlockEntityType.Builder.create((pos, state) -> new SmallTankEntity(pos, state, true), BlockContent.CREATIVE_TANK_BLOCK).build(null);
    
    public static final BlockEntityType<FluidPipeInterfaceEntity> FLUID_PIPE_ENTITY = BlockEntityType.Builder.create(FluidPipeInterfaceEntity::new, BlockContent.FLUID_PIPE_CONNECTION, BlockContent.FRAMED_FLUID_PIPE_CONNECTION).build(null);
    @AssignSidedEnergy
    public static final BlockEntityType<EnergyPipeInterfaceEntity> ENERGY_PIPE_ENTITY = BlockEntityType.Builder.create(EnergyPipeInterfaceEntity::new, BlockContent.ENERGY_PIPE_CONNECTION, BlockContent.SUPERCONDUCTOR_CONNECTION, BlockContent.FRAMED_ENERGY_PIPE_CONNECTION, BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION).build(null);
    public static final BlockEntityType<ItemPipeInterfaceEntity> ITEM_PIPE_ENTITY = BlockEntityType.Builder.create(ItemPipeInterfaceEntity::new, BlockContent.ITEM_PIPE_CONNECTION, BlockContent.FRAMED_ITEM_PIPE_CONNECTION, BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION).build(null);
    @AssignSidedInventory
    public static final BlockEntityType<ItemFilterBlockEntity> ITEM_FILTER_ENTITY = BlockEntityType.Builder.create(ItemFilterBlockEntity::new, BlockContent.ITEM_FILTER_BLOCK).build(null);
    
    
    public static final BlockEntityType<AddonBlockEntity> ADDON_ENTITY = BlockEntityType.Builder.create(AddonBlockEntity::new,
      BlockContent.MACHINE_SPEED_ADDON,
      BlockContent.MACHINE_PROCESSING_ADDON,
      BlockContent.MACHINE_EFFICIENCY_ADDON,
      BlockContent.MACHINE_ULTIMATE_ADDON,
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
    ).build(null);
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final BlockEntityType<MachineCoreEntity> MACHINE_CORE_ENTITY = BlockEntityType.Builder.create(MachineCoreEntity::new,
      BlockContent.MACHINE_CORE_1,
      BlockContent.MACHINE_CORE_2,
      BlockContent.MACHINE_CORE_3,
      BlockContent.MACHINE_CORE_4,
      BlockContent.MACHINE_CORE_5,
      BlockContent.MACHINE_CORE_6,
      BlockContent.MACHINE_CORE_7,
      BlockContent.MACHINE_CORE_HIDDEN
    ).build(null);
    
    public static final BlockEntityType<TechDoorBlockEntity> TECH_DOOR_ENTITY = BlockEntityType.Builder.create(TechDoorBlockEntity::new, BlockContent.TECH_DOOR).build(null);
    
    @Override
    @SuppressWarnings("unchecked")
    public Class<BlockEntityType<?>> getTargetFieldType() {
        return (Class<BlockEntityType<?>>) (Object) BlockEntityType.class;
    }
    
    @Override
    public RegistryKey<Registry<BlockEntityType<?>>> getRegistryType() {
        return RegistryKeys.BLOCK_ENTITY_TYPE;
    }
    
    @Override
    public void postProcessField(String namespace, BlockEntityType<?> value, String identifier, Field field, RegistrySupplier<BlockEntityType<?>> supplier) {
        
        if (EnergyApi.BLOCK != null && field.isAnnotationPresent(AssignSidedEnergy.class))
            EnergyApi.BLOCK.registerBlockEntity(() -> value);
        
        if (FluidApi.BLOCK != null && field.isAnnotationPresent(AssignSidedFluid.class))
            FluidApi.BLOCK.registerBlockEntity(() -> value);
        
        if (ItemApi.BLOCK != null && field.isAnnotationPresent(AssignSidedInventory.class))
            ItemApi.BLOCK.registerBlockEntity(() -> value);
        
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
