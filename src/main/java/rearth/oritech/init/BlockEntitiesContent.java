package rearth.oritech.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class BlockEntitiesContent {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Oritech.MOD_ID);
    
    // target
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> PULVERIZER_ENTITY = BLOCK_ENTITY_TYPES.register("pulverizer_entity", () -> new BlockEntityType<>(PulverizerBlockEntity::new, BlockContent.PULVERIZER_BLOCK.get()));
    
    // old
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> FRAGMENT_FORGE_ENTITY = BLOCK_ENTITY_TYPES.register("fragment_forge_entity", () -> new BlockEntityType<>(FragmentForgeBlockEntity::new, BlockContent.FRAGMENT_FORGE_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> ASSEMBLER_ENTITY = BLOCK_ENTITY_TYPES.register("assembler_entity", () -> new BlockEntityType<>(AssemblerBlockEntity::new, BlockContent.ASSEMBLER_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> FOUNDRY_ENTITY = BLOCK_ENTITY_TYPES.register("foundry_entity", () -> new BlockEntityType<>(FoundryBlockEntity::new, BlockContent.FOUNDRY_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> COOLER_ENTITY = BLOCK_ENTITY_TYPES.register("cooler_entity", () -> new BlockEntityType<>(CoolerBlockEntity::new, BlockContent.COOLER_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> CENTRIFUGE_ENTITY = BLOCK_ENTITY_TYPES.register("centrifuge_entity", () -> new BlockEntityType<>(CentrifugeBlockEntity::new, BlockContent.CENTRIFUGE_BLOCK.get()));
    
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> ATOMIC_FORGE_ENTITY = BLOCK_ENTITY_TYPES.register("atomic_forge_entity", () -> new BlockEntityType<>(AtomicForgeBlockEntity::new, BlockContent.ATOMIC_FORGE_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> REFINERY_ENTITY = BLOCK_ENTITY_TYPES.register("refinery_entity", () -> new BlockEntityType<>(RefineryBlockEntity::new, BlockContent.REFINERY_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> TAINTED_REFINERY_ENTITY = BLOCK_ENTITY_TYPES.register("tainted_refinery_entity", () -> new BlockEntityType<>(TaintedRefineryBlockEntity::new, BlockContent.TAINTED_REFINERY_BLOCK.get()));
    
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> REFINERY_MODULE_ENTITY = BLOCK_ENTITY_TYPES.register("refinery_module_entity", () -> new BlockEntityType<>(RefineryModuleBlockEntity::new, BlockContent.REFINERY_MODULE_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> BIO_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("bio_generator_entity", () -> new BlockEntityType<>(BioGeneratorEntity::new, BlockContent.BIO_GENERATOR_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> LAVA_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("lava_generator_entity", () -> new BlockEntityType<>(LavaGeneratorEntity::new, BlockContent.LAVA_GENERATOR_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> FUEL_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("fuel_generator_entity", () -> new BlockEntityType<>(FuelGeneratorEntity::new, BlockContent.FUEL_GENERATOR_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> BASIC_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("basic_generator_entity", () -> new BlockEntityType<>(BasicGeneratorEntity::new, BlockContent.BASIC_GENERATOR_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> STEAM_ENGINE_ENTITY = BLOCK_ENTITY_TYPES.register("steam_engine_entity", () -> new BlockEntityType<>(SteamEngineEntity::new, BlockContent.STEAM_ENGINE_BLOCK.get()));
    
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> BIG_SOLAR_ENTITY = BLOCK_ENTITY_TYPES.register("big_solar_entity", () -> new BlockEntityType<>(BigSolarPanelEntity::new, BlockContent.BIG_SOLAR_PANEL_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> POWERED_FURNACE_ENTITY = BLOCK_ENTITY_TYPES.register("powered_furnace_entity", () -> new BlockEntityType<>(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> LASER_ARM_ENTITY = BLOCK_ENTITY_TYPES.register("laser_arm_entity", () -> new BlockEntityType<>(LaserArmBlockEntity::new, BlockContent.LASER_ARM_BLOCK.get()));
    
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> DEEP_DRILL_ENTITY = BLOCK_ENTITY_TYPES.register("deep_drill_entity", () -> new BlockEntityType<>(DeepDrillEntity::new, BlockContent.DEEP_DRILL_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> DRONE_PORT_ENTITY = BLOCK_ENTITY_TYPES.register("drone_port_entity", () -> new BlockEntityType<>(DronePortEntity::new, BlockContent.DRONE_PORT_BLOCK.get()));
    
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> PUMP_BLOCK = BLOCK_ENTITY_TYPES.register("pump_block", () -> new BlockEntityType<>(PumpBlockEntity::new, BlockContent.PUMP_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> SHRINKER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("shrinker_block_entity", () -> new BlockEntityType<>(ShrinkerBlockEntity::new, BlockContent.SHRINKER_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> CHARGER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("charger_block_entity", () -> new BlockEntityType<>(ChargerBlockEntity::new, BlockContent.CHARGER_BLOCK.get()));
    
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> ENERGY_ACCEPTOR_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("energy_acceptor_addon_entity", () -> new BlockEntityType<>(EnergyAcceptorAddonBlockEntity::new, BlockContent.MACHINE_ACCEPTOR_ADDON.get()));
    
    public static final Supplier<BlockEntityType<?>> REDSTONE_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("redstone_addon_entity", () -> new BlockEntityType<>(RedstoneAddonBlockEntity::new, BlockContent.MACHINE_REDSTONE_ADDON.get()));
    
    public static final Supplier<BlockEntityType<?>> COMBI_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("combi_addon_entity", () -> new BlockEntityType<>(CombiAddonEntity::new, BlockContent.MACHINE_COMBI_ADDON.get()));
    
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> STEAM_BOILER_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("steam_boiler_addon_entity", () -> new BlockEntityType<>(SteamBoilerAddonBlockEntity::new, BlockContent.STEAM_BOILER_ADDON.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> PLACER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("placer_block_entity", () -> new BlockEntityType<>(PlacerBlockEntity::new, BlockContent.PLACER_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> DESTROYER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("destroyer_block_entity", () -> new BlockEntityType<>(DestroyerBlockEntity::new, BlockContent.DESTROYER_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> FERTILIZER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("fertilizer_block_entity", () -> new BlockEntityType<>(FertilizerBlockEntity::new, BlockContent.FERTILIZER_BLOCK.get()));
    
    @AssignSidedEnergy
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> TREEFELLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("treefeller_block_entity", () -> new BlockEntityType<>(TreefellerBlockEntity::new, BlockContent.TREEFELLER_BLOCK.get()));
    
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> PIPE_BOOSTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("pipe_booster_block_entity", () -> new BlockEntityType<>(PipeBoosterBlockEntity::new, BlockContent.PIPE_BOOSTER_BLOCK.get()));
    
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> ENCHANTMENT_CATALYST_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("enchantment_catalyst_block_entity", () -> new BlockEntityType<>(EnchantmentCatalystBlockEntity::new, BlockContent.ENCHANTMENT_CATALYST_BLOCK.get()));
    
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> UNSTABLE_CONTAINER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("unstable_container_block_entity", () -> new BlockEntityType<>(UnstableContainerBlockEntity::new, BlockContent.UNSTABLE_CONTAINER.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> ENCHANTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("enchanter_block_entity", () -> new BlockEntityType<>(EnchanterBlockEntity::new, BlockContent.ENCHANTER_BLOCK.get()));
    
    public static final Supplier<BlockEntityType<?>> SPAWNER_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("spawner_controller_block_entity", () -> new BlockEntityType<>(SpawnerControllerBlockEntity::new, BlockContent.SPAWNER_CONTROLLER_BLOCK.get()));
    
    public static final Supplier<BlockEntityType<?>> REACTOR_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_controller_block_entity", () -> new BlockEntityType<>(ReactorControllerBlockEntity::new, BlockContent.REACTOR_CONTROLLER.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> REACTOR_FUEL_PORT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_fuel_port_block_entity", () -> new BlockEntityType<>(ReactorFuelPortEntity::new, BlockContent.REACTOR_FUEL_PORT.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> REACTOR_ABSORBER_PORT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_absorber_port_block_entity", () -> new BlockEntityType<>(ReactorAbsorberPortEntity::new, BlockContent.REACTOR_ABSORBER_PORT.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> REACTOR_ENERGY_PORT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_energy_port_block_entity", () -> new BlockEntityType<>(ReactorEnergyPortEntity::new, BlockContent.REACTOR_ENERGY_PORT.get()));
    public static final Supplier<BlockEntityType<?>> REACTOR_EXPLOSION_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_explosion_entity", () -> new BlockEntityType<>(NuclearExplosionEntity::new, BlockContent.REACTOR_EXPLOSION_SMALL.get(), BlockContent.REACTOR_EXPLOSION_MEDIUM.get(), BlockContent.REACTOR_EXPLOSION_LARGE.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> PLAYER_MODIFIER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("player_modifier_block_entity", () -> new BlockEntityType<>(AugmentApplicationEntity::new, BlockContent.AUGMENT_APPLICATION_BLOCK.get()));
    public static final Supplier<BlockEntityType<?>> AUGMENTER_RESEARCH_STATION_ENTITY = BLOCK_ENTITY_TYPES.register("augmenter_research_station_entity", () -> new BlockEntityType<>(AugmentResearchStationBlockEntity::new, BlockContent.SIMPLE_AUGMENT_STATION.get(), BlockContent.ADVANCED_AUGMENT_STATION.get(), BlockContent.ARCANE_AUGMENT_STATION.get()));
    
    
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> ACCELERATOR_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("accelerator_controller_block_entity", () -> new BlockEntityType<>(AcceleratorControllerBlockEntity::new, BlockContent.ACCELERATOR_CONTROLLER.get()));
    public static final Supplier<BlockEntityType<?>> ACCELERATOR_SENSOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("accelerator_sensor_block_entity", () -> new BlockEntityType<>(AcceleratorSensorBlockEntity::new, BlockContent.ACCELERATOR_SENSOR.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> ACCELERATOR_MOTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("accelerator_motor_block_entity", () -> new BlockEntityType<>(AcceleratorMotorBlockEntity::new, BlockContent.ACCELERATOR_MOTOR.get()));
    public static final Supplier<BlockEntityType<?>> BLACK_HOLE_ENTITY = BLOCK_ENTITY_TYPES.register("black_hole_entity", () -> new BlockEntityType<>(BlackHoleBlockEntity::new, BlockContent.BLACK_HOLE_BLOCK.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> PARTICLE_COLLECTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("particle_collector_block_entity", () -> new BlockEntityType<>(ParticleCollectorBlockEntity::new, BlockContent.PARTICLE_COLLECTOR_BLOCK.get()));
    
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> INVENTORY_PROXY_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("inventory_proxy_addon_entity", () -> new BlockEntityType<>(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get()));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> SMALL_STORAGE_ENTITY = BLOCK_ENTITY_TYPES.register("small_storage_entity", () -> new BlockEntityType<>(SmallStorageBlockEntity::new, BlockContent.SMALL_STORAGE_BLOCK.get()));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> LARGE_STORAGE_ENTITY = BLOCK_ENTITY_TYPES.register("large_storage_entity", () -> new BlockEntityType<>(LargeStorageBlockEntity::new, BlockContent.LARGE_STORAGE_BLOCK.get()));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> CREATIVE_STORAGE_ENTITY = BLOCK_ENTITY_TYPES.register("creative_storage_entity", () -> new BlockEntityType<>(CreativeStorageBlockEntity::new, BlockContent.CREATIVE_STORAGE_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> SMALL_TANK_ENTITY = BLOCK_ENTITY_TYPES.register("small_tank_entity", () -> new BlockEntityType<>((pos, state) -> new SmallTankEntity(pos, state, false), BlockContent.SMALL_TANK_BLOCK.get()));
    
    @AssignSidedInventory
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> CREATIVE_TANK_ENTITY = BLOCK_ENTITY_TYPES.register("creative_tank_entity", () -> new BlockEntityType<>((pos, state) -> new SmallTankEntity(pos, state, true), BlockContent.CREATIVE_TANK_BLOCK.get()));
    
    public static final Supplier<BlockEntityType<?>> FLUID_PIPE_ENTITY = BLOCK_ENTITY_TYPES.register("fluid_pipe_entity", () -> new BlockEntityType<>(FluidPipeInterfaceEntity::new, BlockContent.FLUID_PIPE_CONNECTION.get(), BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> ENERGY_PIPE_ENTITY = BLOCK_ENTITY_TYPES.register("energy_pipe_entity", () -> new BlockEntityType<>(EnergyPipeInterfaceEntity::new, BlockContent.ENERGY_PIPE_CONNECTION.get(), BlockContent.SUPERCONDUCTOR_CONNECTION.get(), BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get()));
    public static final Supplier<BlockEntityType<?>> ITEM_PIPE_ENTITY = BLOCK_ENTITY_TYPES.register("item_pipe_entity", () -> new BlockEntityType<>(ItemPipeInterfaceEntity::new, BlockContent.ITEM_PIPE_CONNECTION.get(), BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<?>> ITEM_FILTER_ENTITY = BLOCK_ENTITY_TYPES.register("item_filter_entity", () -> new BlockEntityType<>(ItemFilterBlockEntity::new, BlockContent.ITEM_FILTER_BLOCK.get()));
    
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<?>> POWER_POLE_ENTITY = BLOCK_ENTITY_TYPES.register("power_pole_entity", () -> new BlockEntityType<>(PowerPoleEntity::new, BlockContent.POWER_POLE_BLOCK.get()));
    
    public static final Supplier<BlockEntityType<?>> ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("addon_entity", () -> new BlockEntityType<>(AddonBlockEntity::new,
      BlockContent.MACHINE_SPEED_ADDON.get(),
      BlockContent.MACHINE_PROCESSING_ADDON.get(),
      BlockContent.MACHINE_EFFICIENCY_ADDON.get(),
      BlockContent.MACHINE_ULTIMATE_ADDON.get(),
      BlockContent.MACHINE_FLUID_ADDON.get(),
      BlockContent.MACHINE_HUNTER_ADDON.get(),
      BlockContent.MACHINE_YIELD_ADDON.get(),
      BlockContent.CROP_FILTER_ADDON.get(),
      BlockContent.MACHINE_EXTENDER.get(),
      BlockContent.MACHINE_CAPACITOR_ADDON.get(),
      BlockContent.CAPACITOR_ADDON_EXTENDER.get(),
      BlockContent.QUARRY_ADDON.get(),
      BlockContent.MACHINE_HUNTER_ADDON.get(),
      BlockContent.QUARRY_ADDON.get(),
      BlockContent.MACHINE_SILK_TOUCH_ADDON.get(),
      BlockContent.MACHINE_BURST_ADDON.get()
    ));
    
    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<?>> MACHINE_CORE_ENTITY = BLOCK_ENTITY_TYPES.register("machine_core_entity", () -> new BlockEntityType<>(MachineCoreEntity::new,
      BlockContent.MACHINE_CORE_1.get(),
      BlockContent.MACHINE_CORE_2.get(),
      BlockContent.MACHINE_CORE_3.get(),
      BlockContent.MACHINE_CORE_4.get(),
      BlockContent.MACHINE_CORE_5.get(),
      BlockContent.MACHINE_CORE_6.get(),
      BlockContent.MACHINE_CORE_7.get(),
      BlockContent.MACHINE_CORE_HIDDEN.get()
    ));
    
    public static final Supplier<BlockEntityType<?>> TECH_DOOR_ENTITY = BLOCK_ENTITY_TYPES.register("tech_door_entity", () -> new BlockEntityType<>(TechDoorBlockEntity::new, BlockContent.TECH_DOOR.get()));
    public static final Supplier<BlockEntityType<?>> HANGAR_DOOR_ENTITY = BLOCK_ENTITY_TYPES.register("hangar_door_entity", () -> new BlockEntityType<>(HangarDoorBlockEntity::new, BlockContent.HANGAR_DOOR.get()));
    
    
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
