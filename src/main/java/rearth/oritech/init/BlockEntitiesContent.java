package rearth.oritech.init;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import rearth.oritech.block.entity.decorative.HangarDoorBlockEntity;
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
import rearth.oritech.util.registry.OritechDeferredRegistry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

public class BlockEntitiesContent {

    public static final OritechDeferredRegistry<BlockEntityType<?>> BLOCK_ENTITIES = OritechDeferredRegistry.create(Registries.BLOCK_ENTITY_TYPE);
    private static final Map<RegistrySupplier<BlockEntityType<?>>, BlockEntityType<?>> BLOCK_ENTITY_VALUES = new IdentityHashMap<>();

    private static RegistrySupplier<BlockEntityType<?>> registerBlockEntity(String path, BlockEntityType<?> value) {
        var supplier = BLOCK_ENTITIES.register(path, () -> value);
        BLOCK_ENTITY_VALUES.put(supplier, value);
        return supplier;
    }
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> PULVERIZER_ENTITY = registerBlockEntity("pulverizer_entity", BlockEntityType.Builder.of(PulverizerBlockEntity::new, BlockContent.value(BlockContent.PULVERIZER_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> FRAGMENT_FORGE_ENTITY = registerBlockEntity("fragment_forge_entity", BlockEntityType.Builder.of(FragmentForgeBlockEntity::new, BlockContent.value(BlockContent.FRAGMENT_FORGE_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> ASSEMBLER_ENTITY = registerBlockEntity("assembler_entity", BlockEntityType.Builder.of(AssemblerBlockEntity::new, BlockContent.value(BlockContent.ASSEMBLER_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> FOUNDRY_ENTITY = registerBlockEntity("foundry_entity", BlockEntityType.Builder.of(FoundryBlockEntity::new, BlockContent.value(BlockContent.FOUNDRY_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> COOLER_ENTITY = registerBlockEntity("cooler_entity", BlockEntityType.Builder.of(CoolerBlockEntity::new, BlockContent.value(BlockContent.COOLER_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> CENTRIFUGE_ENTITY = registerBlockEntity("centrifuge_entity", BlockEntityType.Builder.of(CentrifugeBlockEntity::new, BlockContent.value(BlockContent.CENTRIFUGE_BLOCK)).build(null));
    
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> ATOMIC_FORGE_ENTITY = registerBlockEntity("atomic_forge_entity", BlockEntityType.Builder.of(AtomicForgeBlockEntity::new, BlockContent.value(BlockContent.ATOMIC_FORGE_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> REFINERY_ENTITY = registerBlockEntity("refinery_entity", BlockEntityType.Builder.of(RefineryBlockEntity::new, BlockContent.value(BlockContent.REFINERY_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> TAINTED_REFINERY_ENTITY = registerBlockEntity("tainted_refinery_entity", BlockEntityType.Builder.of(TaintedRefineryBlockEntity::new, BlockContent.value(BlockContent.TAINTED_REFINERY_BLOCK)).build(null));
    
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> REFINERY_MODULE_ENTITY = registerBlockEntity("refinery_module_entity", BlockEntityType.Builder.of(RefineryModuleBlockEntity::new, BlockContent.value(BlockContent.REFINERY_MODULE_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> BIO_GENERATOR_ENTITY = registerBlockEntity("bio_generator_entity", BlockEntityType.Builder.of(BioGeneratorEntity::new, BlockContent.value(BlockContent.BIO_GENERATOR_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> LAVA_GENERATOR_ENTITY = registerBlockEntity("lava_generator_entity", BlockEntityType.Builder.of(LavaGeneratorEntity::new, BlockContent.value(BlockContent.LAVA_GENERATOR_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> FUEL_GENERATOR_ENTITY = registerBlockEntity("fuel_generator_entity", BlockEntityType.Builder.of(FuelGeneratorEntity::new, BlockContent.value(BlockContent.FUEL_GENERATOR_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> BASIC_GENERATOR_ENTITY = registerBlockEntity("basic_generator_entity", BlockEntityType.Builder.of(BasicGeneratorEntity::new, BlockContent.value(BlockContent.BASIC_GENERATOR_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> STEAM_ENGINE_ENTITY = registerBlockEntity("steam_engine_entity", BlockEntityType.Builder.of(SteamEngineEntity::new, BlockContent.value(BlockContent.STEAM_ENGINE_BLOCK)).build(null));
    
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> BIG_SOLAR_ENTITY = registerBlockEntity("big_solar_entity", BlockEntityType.Builder.of(BigSolarPanelEntity::new, BlockContent.value(BlockContent.BIG_SOLAR_PANEL_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> POWERED_FURNACE_ENTITY = registerBlockEntity("powered_furnace_entity", BlockEntityType.Builder.of(PoweredFurnaceBlockEntity::new, BlockContent.value(BlockContent.POWERED_FURNACE_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> LASER_ARM_ENTITY = registerBlockEntity("laser_arm_entity", BlockEntityType.Builder.of(LaserArmBlockEntity::new, BlockContent.value(BlockContent.LASER_ARM_BLOCK)).build(null));
    
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> DEEP_DRILL_ENTITY = registerBlockEntity("deep_drill_entity", BlockEntityType.Builder.of(DeepDrillEntity::new, BlockContent.value(BlockContent.DEEP_DRILL_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> DRONE_PORT_ENTITY = registerBlockEntity("drone_port_entity", BlockEntityType.Builder.of(DronePortEntity::new, BlockContent.value(BlockContent.DRONE_PORT_BLOCK)).build(null));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> PUMP_BLOCK = registerBlockEntity("pump_block", BlockEntityType.Builder.of(PumpBlockEntity::new, BlockContent.value(BlockContent.PUMP_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> SHRINKER_BLOCK_ENTITY = registerBlockEntity("shrinker_block_entity", BlockEntityType.Builder.of(ShrinkerBlockEntity::new, BlockContent.value(BlockContent.SHRINKER_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> CHARGER_BLOCK_ENTITY = registerBlockEntity("charger_block_entity", BlockEntityType.Builder.of(ChargerBlockEntity::new, BlockContent.value(BlockContent.CHARGER_BLOCK)).build(null));
    
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> ENERGY_ACCEPTOR_ADDON_ENTITY = registerBlockEntity("energy_acceptor_addon_entity", BlockEntityType.Builder.of(EnergyAcceptorAddonBlockEntity::new, BlockContent.value(BlockContent.MACHINE_ACCEPTOR_ADDON)).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> REDSTONE_ADDON_ENTITY = registerBlockEntity("redstone_addon_entity", BlockEntityType.Builder.of(RedstoneAddonBlockEntity::new, BlockContent.value(BlockContent.MACHINE_REDSTONE_ADDON)).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> COMBI_ADDON_ENTITY = registerBlockEntity("combi_addon_entity", BlockEntityType.Builder.of(CombiAddonEntity::new, BlockContent.value(BlockContent.MACHINE_COMBI_ADDON)).build(null));
    
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> STEAM_BOILER_ADDON_ENTITY = registerBlockEntity("steam_boiler_addon_entity", BlockEntityType.Builder.of(SteamBoilerAddonBlockEntity::new, BlockContent.value(BlockContent.STEAM_BOILER_ADDON)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> PLACER_BLOCK_ENTITY = registerBlockEntity("placer_block_entity", BlockEntityType.Builder.of(PlacerBlockEntity::new, BlockContent.value(BlockContent.PLACER_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> DESTROYER_BLOCK_ENTITY = registerBlockEntity("destroyer_block_entity", BlockEntityType.Builder.of(DestroyerBlockEntity::new, BlockContent.value(BlockContent.DESTROYER_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> FERTILIZER_BLOCK_ENTITY = registerBlockEntity("fertilizer_block_entity", BlockEntityType.Builder.of(FertilizerBlockEntity::new, BlockContent.value(BlockContent.FERTILIZER_BLOCK)).build(null));
    
    @AssignSidedEnergy
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> TREEFELLER_BLOCK_ENTITY = registerBlockEntity("treefeller_block_entity", BlockEntityType.Builder.of(TreefellerBlockEntity::new, BlockContent.value(BlockContent.TREEFELLER_BLOCK)).build(null));
    
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> PIPE_BOOSTER_BLOCK_ENTITY = registerBlockEntity("pipe_booster_block_entity", BlockEntityType.Builder.of(PipeBoosterBlockEntity::new, BlockContent.value(BlockContent.PIPE_BOOSTER_BLOCK)).build(null));
    
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> ENCHANTMENT_CATALYST_BLOCK_ENTITY = registerBlockEntity("enchantment_catalyst_block_entity", BlockEntityType.Builder.of(EnchantmentCatalystBlockEntity::new, BlockContent.value(BlockContent.ENCHANTMENT_CATALYST_BLOCK)).build(null));
    
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> UNSTABLE_CONTAINER_BLOCK_ENTITY = registerBlockEntity("unstable_container_block_entity", BlockEntityType.Builder.of(UnstableContainerBlockEntity::new, BlockContent.value(BlockContent.UNSTABLE_CONTAINER)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> ENCHANTER_BLOCK_ENTITY = registerBlockEntity("enchanter_block_entity", BlockEntityType.Builder.of(EnchanterBlockEntity::new, BlockContent.value(BlockContent.ENCHANTER_BLOCK)).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> SPAWNER_CONTROLLER_BLOCK_ENTITY = registerBlockEntity("spawner_controller_block_entity", BlockEntityType.Builder.of(SpawnerControllerBlockEntity::new, BlockContent.value(BlockContent.SPAWNER_CONTROLLER_BLOCK)).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> REACTOR_CONTROLLER_BLOCK_ENTITY = registerBlockEntity("reactor_controller_block_entity", BlockEntityType.Builder.of(ReactorControllerBlockEntity::new, BlockContent.value(BlockContent.REACTOR_CONTROLLER)).build(null));
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> REACTOR_FUEL_PORT_BLOCK_ENTITY = registerBlockEntity("reactor_fuel_port_block_entity", BlockEntityType.Builder.of(ReactorFuelPortEntity::new, BlockContent.value(BlockContent.REACTOR_FUEL_PORT)).build(null));
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> REACTOR_ABSORBER_PORT_BLOCK_ENTITY = registerBlockEntity("reactor_absorber_port_block_entity", BlockEntityType.Builder.of(ReactorAbsorberPortEntity::new, BlockContent.value(BlockContent.REACTOR_ABSORBER_PORT)).build(null));
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> REACTOR_ENERGY_PORT_BLOCK_ENTITY = registerBlockEntity("reactor_energy_port_block_entity", BlockEntityType.Builder.of(ReactorEnergyPortEntity::new, BlockContent.value(BlockContent.REACTOR_ENERGY_PORT)).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> REACTOR_EXPLOSION_ENTITY = registerBlockEntity("reactor_explosion_entity", BlockEntityType.Builder.of(NuclearExplosionEntity::new, BlockContent.value(BlockContent.REACTOR_EXPLOSION_SMALL), BlockContent.value(BlockContent.REACTOR_EXPLOSION_MEDIUM), BlockContent.value(BlockContent.REACTOR_EXPLOSION_LARGE)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> PLAYER_MODIFIER_BLOCK_ENTITY = registerBlockEntity("player_modifier_block_entity", BlockEntityType.Builder.of(AugmentApplicationEntity::new, BlockContent.value(BlockContent.AUGMENT_APPLICATION_BLOCK)).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> AUGMENTER_RESEARCH_STATION_ENTITY = registerBlockEntity("augmenter_research_station_entity", BlockEntityType.Builder.of(AugmentResearchStationBlockEntity::new, BlockContent.value(BlockContent.SIMPLE_AUGMENT_STATION), BlockContent.value(BlockContent.ADVANCED_AUGMENT_STATION), BlockContent.value(BlockContent.ARCANE_AUGMENT_STATION)).build(null));
    
    
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> ACCELERATOR_CONTROLLER_BLOCK_ENTITY = registerBlockEntity("accelerator_controller_block_entity", BlockEntityType.Builder.of(AcceleratorControllerBlockEntity::new, BlockContent.value(BlockContent.ACCELERATOR_CONTROLLER)).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> ACCELERATOR_SENSOR_BLOCK_ENTITY = registerBlockEntity("accelerator_sensor_block_entity", BlockEntityType.Builder.of(AcceleratorSensorBlockEntity::new, BlockContent.value(BlockContent.ACCELERATOR_SENSOR)).build(null));
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> ACCELERATOR_MOTOR_BLOCK_ENTITY = registerBlockEntity("accelerator_motor_block_entity", BlockEntityType.Builder.of(AcceleratorMotorBlockEntity::new, BlockContent.value(BlockContent.ACCELERATOR_MOTOR)).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> BLACK_HOLE_ENTITY = registerBlockEntity("black_hole_entity", BlockEntityType.Builder.of(BlackHoleBlockEntity::new, BlockContent.value(BlockContent.BLACK_HOLE_BLOCK)).build(null));
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> PARTICLE_COLLECTOR_BLOCK_ENTITY = registerBlockEntity("particle_collector_block_entity", BlockEntityType.Builder.of(ParticleCollectorBlockEntity::new, BlockContent.value(BlockContent.PARTICLE_COLLECTOR_BLOCK)).build(null));
    
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> INVENTORY_PROXY_ADDON_ENTITY = registerBlockEntity("inventory_proxy_addon_entity", BlockEntityType.Builder.of(InventoryProxyAddonBlockEntity::new, BlockContent.value(BlockContent.MACHINE_INVENTORY_PROXY_ADDON)).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> SMALL_STORAGE_ENTITY = registerBlockEntity("small_storage_entity", BlockEntityType.Builder.of(SmallStorageBlockEntity::new, BlockContent.value(BlockContent.SMALL_STORAGE_BLOCK)).build(null));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> LARGE_STORAGE_ENTITY = registerBlockEntity("large_storage_entity", BlockEntityType.Builder.of(LargeStorageBlockEntity::new, BlockContent.value(BlockContent.LARGE_STORAGE_BLOCK)).build(null));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> CREATIVE_STORAGE_ENTITY = registerBlockEntity("creative_storage_entity", BlockEntityType.Builder.of(CreativeStorageBlockEntity::new, BlockContent.value(BlockContent.CREATIVE_STORAGE_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> SMALL_TANK_ENTITY = registerBlockEntity("small_tank_entity", BlockEntityType.Builder.of((pos, state) -> new SmallTankEntity(pos, state, false), BlockContent.value(BlockContent.SMALL_TANK_BLOCK)).build(null));
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> CREATIVE_TANK_ENTITY = registerBlockEntity("creative_tank_entity", BlockEntityType.Builder.of((pos, state) -> new SmallTankEntity(pos, state, true), BlockContent.value(BlockContent.CREATIVE_TANK_BLOCK)).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> FLUID_PIPE_ENTITY = registerBlockEntity("fluid_pipe_entity", BlockEntityType.Builder.of(FluidPipeInterfaceEntity::new, BlockContent.value(BlockContent.FLUID_PIPE_CONNECTION), BlockContent.value(BlockContent.FRAMED_FLUID_PIPE_CONNECTION)).build(null));
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> ENERGY_PIPE_ENTITY = registerBlockEntity("energy_pipe_entity", BlockEntityType.Builder.of(EnergyPipeInterfaceEntity::new, BlockContent.value(BlockContent.ENERGY_PIPE_CONNECTION), BlockContent.value(BlockContent.SUPERCONDUCTOR_CONNECTION), BlockContent.value(BlockContent.FRAMED_ENERGY_PIPE_CONNECTION), BlockContent.value(BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION)).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> ITEM_PIPE_ENTITY = registerBlockEntity("item_pipe_entity", BlockEntityType.Builder.of(ItemPipeInterfaceEntity::new, BlockContent.value(BlockContent.ITEM_PIPE_CONNECTION), BlockContent.value(BlockContent.FRAMED_ITEM_PIPE_CONNECTION), BlockContent.value(BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION)).build(null));
    @AssignSidedInventory
    public static final RegistrySupplier<BlockEntityType<?>> ITEM_FILTER_ENTITY = registerBlockEntity("item_filter_entity", BlockEntityType.Builder.of(ItemFilterBlockEntity::new, BlockContent.value(BlockContent.ITEM_FILTER_BLOCK)).build(null));
    
    @AssignSidedEnergy
    public static final RegistrySupplier<BlockEntityType<?>> POWER_POLE_ENTITY = registerBlockEntity("power_pole_entity", BlockEntityType.Builder.of(PowerPoleEntity::new, BlockContent.value(BlockContent.POWER_POLE_BLOCK)).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> ADDON_ENTITY = registerBlockEntity("addon_entity", BlockEntityType.Builder.of(AddonBlockEntity::new,
      BlockContent.value(BlockContent.MACHINE_SPEED_ADDON),
      BlockContent.value(BlockContent.MACHINE_PROCESSING_ADDON),
      BlockContent.value(BlockContent.MACHINE_EFFICIENCY_ADDON),
      BlockContent.value(BlockContent.MACHINE_ULTIMATE_ADDON),
      BlockContent.value(BlockContent.MACHINE_FLUID_ADDON),
      BlockContent.value(BlockContent.MACHINE_HUNTER_ADDON),
      BlockContent.value(BlockContent.MACHINE_YIELD_ADDON),
      BlockContent.value(BlockContent.CROP_FILTER_ADDON),
      BlockContent.value(BlockContent.MACHINE_EXTENDER),
      BlockContent.value(BlockContent.MACHINE_CAPACITOR_ADDON),
      BlockContent.value(BlockContent.CAPACITOR_ADDON_EXTENDER),
      BlockContent.value(BlockContent.QUARRY_ADDON),
      BlockContent.value(BlockContent.MACHINE_HUNTER_ADDON),
      BlockContent.value(BlockContent.QUARRY_ADDON),
      BlockContent.value(BlockContent.MACHINE_SILK_TOUCH_ADDON),
      BlockContent.value(BlockContent.MACHINE_BURST_ADDON)
    ).build(null));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final RegistrySupplier<BlockEntityType<?>> MACHINE_CORE_ENTITY = registerBlockEntity("machine_core_entity", BlockEntityType.Builder.of(MachineCoreEntity::new,
      BlockContent.value(BlockContent.MACHINE_CORE_1),
      BlockContent.value(BlockContent.MACHINE_CORE_2),
      BlockContent.value(BlockContent.MACHINE_CORE_3),
      BlockContent.value(BlockContent.MACHINE_CORE_4),
      BlockContent.value(BlockContent.MACHINE_CORE_5),
      BlockContent.value(BlockContent.MACHINE_CORE_6),
      BlockContent.value(BlockContent.MACHINE_CORE_7),
      BlockContent.value(BlockContent.MACHINE_CORE_HIDDEN)
    ).build(null));
    
    public static final RegistrySupplier<BlockEntityType<?>> TECH_DOOR_ENTITY = registerBlockEntity("tech_door_entity", BlockEntityType.Builder.of(TechDoorBlockEntity::new, BlockContent.value(BlockContent.TECH_DOOR)).build(null));
    public static final RegistrySupplier<BlockEntityType<?>> HANGAR_DOOR_ENTITY = registerBlockEntity("hangar_door_entity", BlockEntityType.Builder.of(HangarDoorBlockEntity::new, BlockContent.value(BlockContent.HANGAR_DOOR)).build(null));
    
    public static void register() {
        BLOCK_ENTITIES.register();

        for (var field : BlockEntitiesContent.class.getDeclaredFields()) {
            if (!RegistrySupplier.class.isAssignableFrom(field.getType())) continue;

            try {
                field.setAccessible(true);
                var supplier = (RegistrySupplier<BlockEntityType<?>>) field.get(null);
                var value = BLOCK_ENTITY_VALUES.get(supplier);
                if (value == null) continue;

                postProcessField(value, field);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access block entity field: " + field.getName(), e);
            }
        }
    }

    private static void postProcessField(BlockEntityType<?> value, Field field) {
        
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

