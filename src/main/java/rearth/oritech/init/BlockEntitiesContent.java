package rearth.oritech.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.item.ItemProvider;
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
import rearth.oritech.util.RegistryReflectionUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

public class BlockEntitiesContent {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Oritech.MOD_ID);

    // target
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PulverizerBlockEntity>> PULVERIZER_ENTITY = BLOCK_ENTITY_TYPES.register("pulverizer_entity", () -> new BlockEntityType<>(PulverizerBlockEntity::new, BlockContent.PULVERIZER_BLOCK.get()));

    // old
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<FragmentForgeBlockEntity>> FRAGMENT_FORGE_ENTITY = BLOCK_ENTITY_TYPES.register("fragment_forge_entity", () -> new BlockEntityType<>(FragmentForgeBlockEntity::new, BlockContent.FRAGMENT_FORGE_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER_ENTITY = BLOCK_ENTITY_TYPES.register("assembler_entity", () -> new BlockEntityType<>(AssemblerBlockEntity::new, BlockContent.ASSEMBLER_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<FoundryBlockEntity>> FOUNDRY_ENTITY = BLOCK_ENTITY_TYPES.register("foundry_entity", () -> new BlockEntityType<>(FoundryBlockEntity::new, BlockContent.FOUNDRY_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<CoolerBlockEntity>> COOLER_ENTITY = BLOCK_ENTITY_TYPES.register("cooler_entity", () -> new BlockEntityType<>(CoolerBlockEntity::new, BlockContent.COOLER_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_ENTITY = BLOCK_ENTITY_TYPES.register("centrifuge_entity", () -> new BlockEntityType<>(CentrifugeBlockEntity::new, BlockContent.CENTRIFUGE_BLOCK.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<AtomicForgeBlockEntity>> ATOMIC_FORGE_ENTITY = BLOCK_ENTITY_TYPES.register("atomic_forge_entity", () -> new BlockEntityType<>(AtomicForgeBlockEntity::new, BlockContent.ATOMIC_FORGE_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<RefineryBlockEntity>> REFINERY_ENTITY = BLOCK_ENTITY_TYPES.register("refinery_entity", () -> new BlockEntityType<>(RefineryBlockEntity::new, BlockContent.REFINERY_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<TaintedRefineryBlockEntity>> TAINTED_REFINERY_ENTITY = BLOCK_ENTITY_TYPES.register("tainted_refinery_entity", () -> new BlockEntityType<>(TaintedRefineryBlockEntity::new, BlockContent.TAINTED_REFINERY_BLOCK.get()));

    @AssignSidedFluid
    public static final Supplier<BlockEntityType<RefineryModuleBlockEntity>> REFINERY_MODULE_ENTITY = BLOCK_ENTITY_TYPES.register("refinery_module_entity", () -> new BlockEntityType<>(RefineryModuleBlockEntity::new, BlockContent.REFINERY_MODULE_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<BioGeneratorEntity>> BIO_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("bio_generator_entity", () -> new BlockEntityType<>(BioGeneratorEntity::new, BlockContent.BIO_GENERATOR_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<LavaGeneratorEntity>> LAVA_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("lava_generator_entity", () -> new BlockEntityType<>(LavaGeneratorEntity::new, BlockContent.LAVA_GENERATOR_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<FuelGeneratorEntity>> FUEL_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("fuel_generator_entity", () -> new BlockEntityType<>(FuelGeneratorEntity::new, BlockContent.FUEL_GENERATOR_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<BasicGeneratorEntity>> BASIC_GENERATOR_ENTITY = BLOCK_ENTITY_TYPES.register("basic_generator_entity", () -> new BlockEntityType<>(BasicGeneratorEntity::new, BlockContent.BASIC_GENERATOR_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<SteamEngineEntity>> STEAM_ENGINE_ENTITY = BLOCK_ENTITY_TYPES.register("steam_engine_entity", () -> new BlockEntityType<>(SteamEngineEntity::new, BlockContent.STEAM_ENGINE_BLOCK.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<BigSolarPanelEntity>> BIG_SOLAR_ENTITY = BLOCK_ENTITY_TYPES.register("big_solar_entity", () -> new BlockEntityType<>(BigSolarPanelEntity::new, BlockContent.BIG_SOLAR_PANEL_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PoweredFurnaceBlockEntity>> POWERED_FURNACE_ENTITY = BLOCK_ENTITY_TYPES.register("powered_furnace_entity", () -> new BlockEntityType<>(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<LaserArmBlockEntity>> LASER_ARM_ENTITY = BLOCK_ENTITY_TYPES.register("laser_arm_entity", () -> new BlockEntityType<>(LaserArmBlockEntity::new, BlockContent.LASER_ARM_BLOCK.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<DeepDrillEntity>> DEEP_DRILL_ENTITY = BLOCK_ENTITY_TYPES.register("deep_drill_entity", () -> new BlockEntityType<>(DeepDrillEntity::new, BlockContent.DEEP_DRILL_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<DronePortEntity>> DRONE_PORT_ENTITY = BLOCK_ENTITY_TYPES.register("drone_port_entity", () -> new BlockEntityType<>(DronePortEntity::new, BlockContent.DRONE_PORT_BLOCK.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PumpBlockEntity>> PUMP_BLOCK = BLOCK_ENTITY_TYPES.register("pump_block", () -> new BlockEntityType<>(PumpBlockEntity::new, BlockContent.PUMP_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<ShrinkerBlockEntity>> SHRINKER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("shrinker_block_entity", () -> new BlockEntityType<>(ShrinkerBlockEntity::new, BlockContent.SHRINKER_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<ChargerBlockEntity>> CHARGER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("charger_block_entity", () -> new BlockEntityType<>(ChargerBlockEntity::new, BlockContent.CHARGER_BLOCK.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EnergyAcceptorAddonBlockEntity>> ENERGY_ACCEPTOR_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("energy_acceptor_addon_entity", () -> new BlockEntityType<>(EnergyAcceptorAddonBlockEntity::new, BlockContent.MACHINE_ACCEPTOR_ADDON.get()));

    public static final Supplier<BlockEntityType<RedstoneAddonBlockEntity>> REDSTONE_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("redstone_addon_entity", () -> new BlockEntityType<>(RedstoneAddonBlockEntity::new, BlockContent.MACHINE_REDSTONE_ADDON.get()));

    public static final Supplier<BlockEntityType<CombiAddonEntity>> COMBI_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("combi_addon_entity", () -> new BlockEntityType<>(CombiAddonEntity::new, BlockContent.MACHINE_COMBI_ADDON.get()));

    @AssignSidedFluid
    public static final Supplier<BlockEntityType<SteamBoilerAddonBlockEntity>> STEAM_BOILER_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("steam_boiler_addon_entity", () -> new BlockEntityType<>(SteamBoilerAddonBlockEntity::new, BlockContent.STEAM_BOILER_ADDON.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PlacerBlockEntity>> PLACER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("placer_block_entity", () -> new BlockEntityType<>(PlacerBlockEntity::new, BlockContent.PLACER_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<DestroyerBlockEntity>> DESTROYER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("destroyer_block_entity", () -> new BlockEntityType<>(DestroyerBlockEntity::new, BlockContent.DESTROYER_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<FertilizerBlockEntity>> FERTILIZER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("fertilizer_block_entity", () -> new BlockEntityType<>(FertilizerBlockEntity::new, BlockContent.FERTILIZER_BLOCK.get()));

    @AssignSidedEnergy
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<TreefellerBlockEntity>> TREEFELLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("treefeller_block_entity", () -> new BlockEntityType<>(TreefellerBlockEntity::new, BlockContent.TREEFELLER_BLOCK.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PipeBoosterBlockEntity>> PIPE_BOOSTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("pipe_booster_block_entity", () -> new BlockEntityType<>(PipeBoosterBlockEntity::new, BlockContent.PIPE_BOOSTER_BLOCK.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<EnchantmentCatalystBlockEntity>> ENCHANTMENT_CATALYST_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("enchantment_catalyst_block_entity", () -> new BlockEntityType<>(EnchantmentCatalystBlockEntity::new, BlockContent.ENCHANTMENT_CATALYST_BLOCK.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<UnstableContainerBlockEntity>> UNSTABLE_CONTAINER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("unstable_container_block_entity", () -> new BlockEntityType<>(UnstableContainerBlockEntity::new, BlockContent.UNSTABLE_CONTAINER.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EnchanterBlockEntity>> ENCHANTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("enchanter_block_entity", () -> new BlockEntityType<>(EnchanterBlockEntity::new, BlockContent.ENCHANTER_BLOCK.get()));

    public static final Supplier<BlockEntityType<SpawnerControllerBlockEntity>> SPAWNER_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("spawner_controller_block_entity", () -> new BlockEntityType<>(SpawnerControllerBlockEntity::new, BlockContent.SPAWNER_CONTROLLER_BLOCK.get()));

    public static final Supplier<BlockEntityType<ReactorControllerBlockEntity>> REACTOR_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_controller_block_entity", () -> new BlockEntityType<>(ReactorControllerBlockEntity::new, BlockContent.REACTOR_CONTROLLER.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ReactorFuelPortEntity>> REACTOR_FUEL_PORT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_fuel_port_block_entity", () -> new BlockEntityType<>(ReactorFuelPortEntity::new, BlockContent.REACTOR_FUEL_PORT.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ReactorAbsorberPortEntity>> REACTOR_ABSORBER_PORT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_absorber_port_block_entity", () -> new BlockEntityType<>(ReactorAbsorberPortEntity::new, BlockContent.REACTOR_ABSORBER_PORT.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<ReactorEnergyPortEntity>> REACTOR_ENERGY_PORT_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_energy_port_block_entity", () -> new BlockEntityType<>(ReactorEnergyPortEntity::new, BlockContent.REACTOR_ENERGY_PORT.get()));
    public static final Supplier<BlockEntityType<NuclearExplosionEntity>> REACTOR_EXPLOSION_ENTITY = BLOCK_ENTITY_TYPES.register("reactor_explosion_entity", () -> new BlockEntityType<>(NuclearExplosionEntity::new, BlockContent.REACTOR_EXPLOSION_SMALL.get(), BlockContent.REACTOR_EXPLOSION_MEDIUM.get(), BlockContent.REACTOR_EXPLOSION_LARGE.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<AugmentApplicationEntity>> PLAYER_MODIFIER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("player_modifier_block_entity", () -> new BlockEntityType<>(AugmentApplicationEntity::new, BlockContent.AUGMENT_APPLICATION_BLOCK.get()));
    public static final Supplier<BlockEntityType<AugmentResearchStationBlockEntity>> AUGMENTER_RESEARCH_STATION_ENTITY = BLOCK_ENTITY_TYPES.register("augmenter_research_station_entity", () -> new BlockEntityType<>(AugmentResearchStationBlockEntity::new, BlockContent.SIMPLE_AUGMENT_STATION.get(), BlockContent.ADVANCED_AUGMENT_STATION.get(), BlockContent.ARCANE_AUGMENT_STATION.get()));


    @AssignSidedInventory
    public static final Supplier<BlockEntityType<AcceleratorControllerBlockEntity>> ACCELERATOR_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("accelerator_controller_block_entity", () -> new BlockEntityType<>(AcceleratorControllerBlockEntity::new, BlockContent.ACCELERATOR_CONTROLLER.get()));
    public static final Supplier<BlockEntityType<AcceleratorSensorBlockEntity>> ACCELERATOR_SENSOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("accelerator_sensor_block_entity", () -> new BlockEntityType<>(AcceleratorSensorBlockEntity::new, BlockContent.ACCELERATOR_SENSOR.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<AcceleratorMotorBlockEntity>> ACCELERATOR_MOTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("accelerator_motor_block_entity", () -> new BlockEntityType<>(AcceleratorMotorBlockEntity::new, BlockContent.ACCELERATOR_MOTOR.get()));
    public static final Supplier<BlockEntityType<BlackHoleBlockEntity>> BLACK_HOLE_ENTITY = BLOCK_ENTITY_TYPES.register("black_hole_entity", () -> new BlockEntityType<>(BlackHoleBlockEntity::new, BlockContent.BLACK_HOLE_BLOCK.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<ParticleCollectorBlockEntity>> PARTICLE_COLLECTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("particle_collector_block_entity", () -> new BlockEntityType<>(ParticleCollectorBlockEntity::new, BlockContent.PARTICLE_COLLECTOR_BLOCK.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<InventoryProxyAddonBlockEntity>> INVENTORY_PROXY_ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("inventory_proxy_addon_entity", () -> new BlockEntityType<>(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<SmallStorageBlockEntity>> SMALL_STORAGE_ENTITY = BLOCK_ENTITY_TYPES.register("small_storage_entity", () -> new BlockEntityType<>(SmallStorageBlockEntity::new, BlockContent.SMALL_STORAGE_BLOCK.get()));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<LargeStorageBlockEntity>> LARGE_STORAGE_ENTITY = BLOCK_ENTITY_TYPES.register("large_storage_entity", () -> new BlockEntityType<>(LargeStorageBlockEntity::new, BlockContent.LARGE_STORAGE_BLOCK.get()));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<CreativeStorageBlockEntity>> CREATIVE_STORAGE_ENTITY = BLOCK_ENTITY_TYPES.register("creative_storage_entity", () -> new BlockEntityType<>(CreativeStorageBlockEntity::new, BlockContent.CREATIVE_STORAGE_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<SmallTankEntity>> SMALL_TANK_ENTITY = BLOCK_ENTITY_TYPES.register("small_tank_entity", () -> new BlockEntityType<>((pos, state) -> new SmallTankEntity(pos, state, false), BlockContent.SMALL_TANK_BLOCK.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<SmallTankEntity>> CREATIVE_TANK_ENTITY = BLOCK_ENTITY_TYPES.register("creative_tank_entity", () -> new BlockEntityType<>((pos, state) -> new SmallTankEntity(pos, state, true), BlockContent.CREATIVE_TANK_BLOCK.get()));

    public static final Supplier<BlockEntityType<FluidPipeInterfaceEntity>> FLUID_PIPE_ENTITY = BLOCK_ENTITY_TYPES.register("fluid_pipe_entity", () -> new BlockEntityType<>(FluidPipeInterfaceEntity::new, BlockContent.FLUID_PIPE_CONNECTION.get(), BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EnergyPipeInterfaceEntity>> ENERGY_PIPE_ENTITY = BLOCK_ENTITY_TYPES.register("energy_pipe_entity", () -> new BlockEntityType<>(EnergyPipeInterfaceEntity::new, BlockContent.ENERGY_PIPE_CONNECTION.get(), BlockContent.SUPERCONDUCTOR_CONNECTION.get(), BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get()));
    public static final Supplier<BlockEntityType<ItemPipeInterfaceEntity>> ITEM_PIPE_ENTITY = BLOCK_ENTITY_TYPES.register("item_pipe_entity", () -> new BlockEntityType<>(ItemPipeInterfaceEntity::new, BlockContent.ITEM_PIPE_CONNECTION.get(), BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ItemFilterBlockEntity>> ITEM_FILTER_ENTITY = BLOCK_ENTITY_TYPES.register("item_filter_entity", () -> new BlockEntityType<>(ItemFilterBlockEntity::new, BlockContent.ITEM_FILTER_BLOCK.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PowerPoleEntity>> POWER_POLE_ENTITY = BLOCK_ENTITY_TYPES.register("power_pole_entity", () -> new BlockEntityType<>(PowerPoleEntity::new, BlockContent.POWER_POLE_BLOCK.get()));

    public static final Supplier<BlockEntityType<AddonBlockEntity>> ADDON_ENTITY = BLOCK_ENTITY_TYPES.register("addon_entity", () -> new BlockEntityType<>(AddonBlockEntity::new,
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
            BlockContent.MACHINE_SILK_TOUCH_ADDON.get(),
            BlockContent.MACHINE_BURST_ADDON.get()
    ));

    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<MachineCoreEntity>> MACHINE_CORE_ENTITY = BLOCK_ENTITY_TYPES.register("machine_core_entity", () -> new BlockEntityType<>(MachineCoreEntity::new,
            BlockContent.MACHINE_CORE_1.get(),
            BlockContent.MACHINE_CORE_2.get(),
            BlockContent.MACHINE_CORE_3.get(),
            BlockContent.MACHINE_CORE_4.get(),
            BlockContent.MACHINE_CORE_5.get(),
            BlockContent.MACHINE_CORE_6.get(),
            BlockContent.MACHINE_CORE_7.get(),
            BlockContent.MACHINE_CORE_HIDDEN.get()
    ));

    public static final Supplier<BlockEntityType<TechDoorBlockEntity>> TECH_DOOR_ENTITY = BLOCK_ENTITY_TYPES.register("tech_door_entity", () -> new BlockEntityType<>(TechDoorBlockEntity::new, BlockContent.TECH_DOOR.get()));
    public static final Supplier<BlockEntityType<HangarDoorBlockEntity>> HANGAR_DOOR_ENTITY = BLOCK_ENTITY_TYPES.register("hangar_door_entity", () -> new BlockEntityType<>(HangarDoorBlockEntity::new, BlockContent.HANGAR_DOOR.get()));


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

    @SuppressWarnings("unchecked")
    public static void registerBlockEntityCapabilities(RegisterCapabilitiesEvent event) {

        RegistryReflectionUtil.IterateFields(
                BlockEntitiesContent.class,
                Supplier.class,
                (field, identifier, value) -> {

                    // Cast the raw generic to the exact type expected by the event
                    var blockEntityTypeSupplier = (Supplier<BlockEntityType<? extends BlockEntity>>) value;

                    if (field.isAnnotationPresent(BlockEntitiesContent.AssignSidedEnergy.class)) {
                        event.registerBlockEntity(
                                Capabilities.Energy.BLOCK,
                                blockEntityTypeSupplier.get(),
                                (entity, side) -> ((EnergyProvider) entity).getEnergyLookup(side));
                    }

                    if (field.isAnnotationPresent(BlockEntitiesContent.AssignSidedInventory.class)) {
                        event.registerBlockEntity(
                                Capabilities.Item.BLOCK,
                                blockEntityTypeSupplier.get(),
                                (entity, side) -> ((ItemProvider) entity).getItemLookup(side));
                    }

                    if (field.isAnnotationPresent(BlockEntitiesContent.AssignSidedFluid.class)) {
                        event.registerBlockEntity(
                                Capabilities.Fluid.BLOCK,
                                blockEntityTypeSupplier.get(),
                                (entity, side) -> ((FluidProvider) entity).getFluidLookup(side));
                    }
                }
        );
    }

}
