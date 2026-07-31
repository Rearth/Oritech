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
import rearth.oritech.block.entity.arcane.StabilizedEnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.ArcaneCatalystBlockEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.block.entity.augmenter.CyberneticAugmentationCenterEntity;
import rearth.oritech.block.entity.augmenter.AugmentResearchStationBlockEntity;
import rearth.oritech.block.entity.decorative.HangarDoorBlockEntity;
import rearth.oritech.block.entity.decorative.IndustrialDoorBlockEntity;
import rearth.oritech.block.entity.generators.*;
import rearth.oritech.block.entity.interaction.*;
import rearth.oritech.block.entity.pipes.EnergyPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.FluidPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.SmartSplitterBlockEntity;
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
    public static final Supplier<BlockEntityType<PulverizerBlockEntity>> PULVERIZER = BLOCK_ENTITY_TYPES.register("pulverizer", () -> new BlockEntityType<>(PulverizerBlockEntity::new, BlockContent.PULVERIZER.get()));

    // old
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<FragmentForgeBlockEntity>> FRAGMENT_FORGE = BLOCK_ENTITY_TYPES.register("fragment_forge", () -> new BlockEntityType<>(FragmentForgeBlockEntity::new, BlockContent.FRAGMENT_FORGE.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<AssemblerBlockEntity>> ASSEMBLER = BLOCK_ENTITY_TYPES.register("assembler", () -> new BlockEntityType<>(AssemblerBlockEntity::new, BlockContent.ASSEMBLER.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<FoundryBlockEntity>> FOUNDRY = BLOCK_ENTITY_TYPES.register("foundry", () -> new BlockEntityType<>(FoundryBlockEntity::new, BlockContent.FOUNDRY.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<IndustrialChillerBlockEntity>> INDUSTRIAL_CHILLER = BLOCK_ENTITY_TYPES.register("industrial_chiller", () -> new BlockEntityType<>(IndustrialChillerBlockEntity::new, BlockContent.INDUSTRIAL_CHILLER.get()));

    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE = BLOCK_ENTITY_TYPES.register("centrifuge", () -> new BlockEntityType<>(CentrifugeBlockEntity::new, BlockContent.CENTRIFUGE.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<AtomicForgeBlockEntity>> ATOMIC_FORGE = BLOCK_ENTITY_TYPES.register("atomic_forge", () -> new BlockEntityType<>(AtomicForgeBlockEntity::new, BlockContent.ATOMIC_FORGE.get()));

    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<RefineryBlockEntity>> REFINERY = BLOCK_ENTITY_TYPES.register("refinery", () -> new BlockEntityType<>(RefineryBlockEntity::new, BlockContent.REFINERY.get()));

    @AssignSidedFluid
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<TaintedRefineryBlockEntity>> TAINTED_REFINERY = BLOCK_ENTITY_TYPES.register("tainted_refinery", () -> new BlockEntityType<>(TaintedRefineryBlockEntity::new, BlockContent.TAINTED_REFINERY.get()));

    @AssignSidedFluid
    public static final Supplier<BlockEntityType<RefineryChamberModuleBlockEntity>> REFINERY_CHAMBER_MODULE = BLOCK_ENTITY_TYPES.register("refinery_chamber_module", () -> new BlockEntityType<>(RefineryChamberModuleBlockEntity::new, BlockContent.REFINERY_CHAMBER_MODULE.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<BioGeneratorEntity>> BIO_GENERATOR = BLOCK_ENTITY_TYPES.register("bio_generator", () -> new BlockEntityType<>(BioGeneratorEntity::new, BlockContent.BIO_GENERATOR.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<LavaGeneratorEntity>> LAVA_GENERATOR = BLOCK_ENTITY_TYPES.register("lava_generator", () -> new BlockEntityType<>(LavaGeneratorEntity::new, BlockContent.LAVA_GENERATOR.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<FuelGeneratorEntity>> FUEL_GENERATOR = BLOCK_ENTITY_TYPES.register("fuel_generator", () -> new BlockEntityType<>(FuelGeneratorEntity::new, BlockContent.FUEL_GENERATOR.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<BasicGeneratorEntity>> BASIC_GENERATOR = BLOCK_ENTITY_TYPES.register("basic_generator", () -> new BlockEntityType<>(BasicGeneratorEntity::new, BlockContent.BASIC_GENERATOR.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<SteamEngineEntity>> STEAM_ENGINE = BLOCK_ENTITY_TYPES.register("steam_engine", () -> new BlockEntityType<>(SteamEngineEntity::new, BlockContent.STEAM_ENGINE.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<BigSolarPanelEntity>> BIG_SOLAR = BLOCK_ENTITY_TYPES.register("big_solar", () -> new BlockEntityType<>(BigSolarPanelEntity::new, BlockContent.BIG_SOLAR_PANEL.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PoweredFurnaceBlockEntity>> POWERED_FURNACE = BLOCK_ENTITY_TYPES.register("powered_furnace", () -> new BlockEntityType<>(PoweredFurnaceBlockEntity::new, BlockContent.POWERED_FURNACE.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EndericLaserBlockEntity>> ENDERIC_LASER = BLOCK_ENTITY_TYPES.register("enderic_laser", () -> new BlockEntityType<>(EndericLaserBlockEntity::new, BlockContent.ENDERIC_LASER.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<BedrockExtractorEntity>> BEDROCK_EXTRACTOR = BLOCK_ENTITY_TYPES.register("bedrock_extractor", () -> new BlockEntityType<>(BedrockExtractorEntity::new, BlockContent.BEDROCK_EXTRACTOR.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<DronePortEntity>> DRONE_PORT = BLOCK_ENTITY_TYPES.register("drone_port", () -> new BlockEntityType<>(DronePortEntity::new, BlockContent.DRONE_PORT.get()));

    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PumpBlockEntity>> PUMP = BLOCK_ENTITY_TYPES.register("pump", () -> new BlockEntityType<>(PumpBlockEntity::new, BlockContent.PUMP.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<AddonSplicerBlockEntity>> ADDON_SPLICER = BLOCK_ENTITY_TYPES.register("addon_splicer", () -> new BlockEntityType<>(AddonSplicerBlockEntity::new, BlockContent.ADDON_SPLICER.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EquipmentChargerBlockEntity>> EQUIPMENT_CHARGER = BLOCK_ENTITY_TYPES.register("equipment_charger", () -> new BlockEntityType<>(EquipmentChargerBlockEntity::new, BlockContent.EQUIPMENT_CHARGER.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EnergyAcceptorAddonBlockEntity>> ENERGY_ACCEPTOR_ADDON = BLOCK_ENTITY_TYPES.register("energy_acceptor_addon", () -> new BlockEntityType<>(EnergyAcceptorAddonBlockEntity::new, BlockContent.MACHINE_ACCEPTOR_ADDON.get()));

    public static final Supplier<BlockEntityType<RedstoneAddonBlockEntity>> REDSTONE_ADDON = BLOCK_ENTITY_TYPES.register("redstone_addon", () -> new BlockEntityType<>(RedstoneAddonBlockEntity::new, BlockContent.CONTROL_UNIT_ADDON.get()));

    public static final Supplier<BlockEntityType<HeartOfTheMachineAddonEntity>> HEART_OF_THE_MACHINE_ADDON = BLOCK_ENTITY_TYPES.register("heart_of_the_machine_addon", () -> new BlockEntityType<>(HeartOfTheMachineAddonEntity::new, BlockContent.HEART_OF_THE_MACHINE_ADDON.get()));

    @AssignSidedFluid
    public static final Supplier<BlockEntityType<SteamBoilerAddonBlockEntity>> STEAM_BOILER_ADDON = BLOCK_ENTITY_TYPES.register("steam_boiler_addon", () -> new BlockEntityType<>(SteamBoilerAddonBlockEntity::new, BlockContent.STEAM_BOILER_ADDON.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PlacerBlockEntity>> PLACER = BLOCK_ENTITY_TYPES.register("placer", () -> new BlockEntityType<>(PlacerBlockEntity::new, BlockContent.PLACER.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<DestroyerBlockEntity>> DESTROYER = BLOCK_ENTITY_TYPES.register("destroyer", () -> new BlockEntityType<>(DestroyerBlockEntity::new, BlockContent.DESTROYER.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<FertilizerBlockEntity>> FERTILIZER = BLOCK_ENTITY_TYPES.register("fertilizer", () -> new BlockEntityType<>(FertilizerBlockEntity::new, BlockContent.FERTILIZER.get()));

    @AssignSidedEnergy
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<TreeCutterBlockEntity>> TREE_CUTTER = BLOCK_ENTITY_TYPES.register("tree_cutter", () -> new BlockEntityType<>(TreeCutterBlockEntity::new, BlockContent.TREE_CUTTER.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PipeBoosterBlockEntity>> PIPE_BOOSTER = BLOCK_ENTITY_TYPES.register("pipe_booster", () -> new BlockEntityType<>(PipeBoosterBlockEntity::new, BlockContent.PIPE_BOOSTER.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ArcaneCatalystBlockEntity>> ARCANE_CATALYST_BLOCK = BLOCK_ENTITY_TYPES.register("arcane_catalyst_block", () -> new BlockEntityType<>(ArcaneCatalystBlockEntity::new, BlockContent.ARCANE_CATALYST.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<SchrodingersSafeBlockEntity>> SCHRODINGERS_SAFE_BLOCK = BLOCK_ENTITY_TYPES.register("schrodingers_safe_block", () -> new BlockEntityType<>(SchrodingersSafeBlockEntity::new, BlockContent.SCHRODINGERS_SAFE.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<StabilizedEnchanterBlockEntity>> STABILIZED_ENCHANTER = BLOCK_ENTITY_TYPES.register("stabilized_enchanter", () -> new BlockEntityType<>(StabilizedEnchanterBlockEntity::new, BlockContent.STABILIZED_ENCHANTER.get()));

    public static final Supplier<BlockEntityType<SpawnerControllerBlockEntity>> SPAWNER_CONTROLLER = BLOCK_ENTITY_TYPES.register("spawner_controller", () -> new BlockEntityType<>(SpawnerControllerBlockEntity::new, BlockContent.SPAWNER_CONTROLLER.get()));

    public static final Supplier<BlockEntityType<NuclearReactorControllerBlockEntity>> NUCLEAR_REACTOR_CONTROLLER_BLOCK = BLOCK_ENTITY_TYPES.register("nuclear_reactor_controller_block", () -> new BlockEntityType<>(NuclearReactorControllerBlockEntity::new, BlockContent.NUCLEAR_REACTOR_CONTROLLER.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ReactorFuelPortEntity>> REACTOR_FUEL_PORT_BLOCK = BLOCK_ENTITY_TYPES.register("reactor_fuel_port_block", () -> new BlockEntityType<>(ReactorFuelPortEntity::new, BlockContent.REACTOR_FUEL_PORT.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ReactorCoolantAbsorberPortEntity>> REACTOR_COOLANT_ABSORBER_PORT_BLOCK = BLOCK_ENTITY_TYPES.register("reactor_coolant_absorber_port_block", () -> new BlockEntityType<>(ReactorCoolantAbsorberPortEntity::new, BlockContent.REACTOR_COOLANT_ABSORBER_PORT.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<ReactorEnergyPortEntity>> REACTOR_ENERGY_PORT_BLOCK = BLOCK_ENTITY_TYPES.register("reactor_energy_port_block", () -> new BlockEntityType<>(ReactorEnergyPortEntity::new, BlockContent.REACTOR_ENERGY_PORT.get()));
    public static final Supplier<BlockEntityType<NuclearExplosionEntity>> REACTOR_EXPLOSION = BLOCK_ENTITY_TYPES.register("reactor_explosion", () -> new BlockEntityType<>(NuclearExplosionEntity::new, BlockContent.REACTOR_EXPLOSION_SMALL.get(), BlockContent.REACTOR_EXPLOSION_MEDIUM.get(), BlockContent.REACTOR_EXPLOSION_LARGE.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<CyberneticAugmentationCenterEntity>> CYBERNETIC_AUGMENTATION_CENTER_BLOCK = BLOCK_ENTITY_TYPES.register("cybernetic_augmentation_center_block", () -> new BlockEntityType<>(CyberneticAugmentationCenterEntity::new, BlockContent.CYBERNETIC_AUGMENTATION_CENTER.get()));
    public static final Supplier<BlockEntityType<AugmentResearchStationBlockEntity>> AUGMENTER_RESEARCH_STATION = BLOCK_ENTITY_TYPES.register("augmenter_research_station", () -> new BlockEntityType<>(AugmentResearchStationBlockEntity::new, BlockContent.CYBERNETIC_RESEARCH_STATION.get(), BlockContent.QUANTUM_RESEARCH_STATION.get(), BlockContent.ARCANE_AUGMENT_STATION.get()));


    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ParticleAcceleratorBlockEntity>> PARTICLE_ACCELERATOR_BLOCK = BLOCK_ENTITY_TYPES.register("particle_accelerator_block", () -> new BlockEntityType<>(ParticleAcceleratorBlockEntity::new, BlockContent.PARTICLE_ACCELERATOR.get()));
    public static final Supplier<BlockEntityType<AcceleratorSensorBlockEntity>> ACCELERATOR_SENSOR_BLOCK = BLOCK_ENTITY_TYPES.register("accelerator_sensor_block", () -> new BlockEntityType<>(AcceleratorSensorBlockEntity::new, BlockContent.ACCELERATOR_SENSOR.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<AcceleratorMotorBlockEntity>> ACCELERATOR_MOTOR_BLOCK = BLOCK_ENTITY_TYPES.register("accelerator_motor_block", () -> new BlockEntityType<>(AcceleratorMotorBlockEntity::new, BlockContent.ACCELERATOR_MOTOR.get()));
    public static final Supplier<BlockEntityType<BlackHoleBlockEntity>> BLACK_HOLE = BLOCK_ENTITY_TYPES.register("black_hole", () -> new BlockEntityType<>(BlackHoleBlockEntity::new, BlockContent.BLACK_HOLE.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<TachyonAbsorberBlockEntity>> TACHYON_ABSORBER_BLOCK = BLOCK_ENTITY_TYPES.register("tachyon_absorber_block", () -> new BlockEntityType<>(TachyonAbsorberBlockEntity::new, BlockContent.TACHYON_ABSORBER.get()));

    @AssignSidedInventory
    public static final Supplier<BlockEntityType<InventoryProxyAddonBlockEntity>> INVENTORY_PROXY_ADDON = BLOCK_ENTITY_TYPES.register("inventory_proxy_addon", () -> new BlockEntityType<>(InventoryProxyAddonBlockEntity::new, BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get()));

    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<PortableEnergyStorageBlockEntity>> PORTABLE_ENERGY_STORAGE = BLOCK_ENTITY_TYPES.register("portable_energy_storage", () -> new BlockEntityType<>(PortableEnergyStorageBlockEntity::new, BlockContent.PORTABLE_ENERGY_STORAGE.get()));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<LargeStorageBlockEntity>> LARGE_STORAGE = BLOCK_ENTITY_TYPES.register("large_storage", () -> new BlockEntityType<>(LargeStorageBlockEntity::new, BlockContent.LARGE_STORAGE.get()));
    @AssignSidedInventory
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<CreativeStorageBlockEntity>> CREATIVE_STORAGE = BLOCK_ENTITY_TYPES.register("creative_storage", () -> new BlockEntityType<>(CreativeStorageBlockEntity::new, BlockContent.CREATIVE_STORAGE.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<PortableTankEntity>> PORTABLE_TANK = BLOCK_ENTITY_TYPES.register("portable_tank", () -> new BlockEntityType<>((pos, state) -> new PortableTankEntity(pos, state, false), BlockContent.PORTABLE_TANK.get()));

    @AssignSidedInventory
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<PortableTankEntity>> CREATIVE_TANK = BLOCK_ENTITY_TYPES.register("creative_tank", () -> new BlockEntityType<>((pos, state) -> new PortableTankEntity(pos, state, true), BlockContent.CREATIVE_TANK.get()));

    public static final Supplier<BlockEntityType<FluidPipeInterfaceEntity>> FLUID_PIPE = BLOCK_ENTITY_TYPES.register("fluid_pipe", () -> new BlockEntityType<>(FluidPipeInterfaceEntity::new, BlockContent.FLUID_PIPE_CONNECTION.get(), BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get()));
    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EnergyPipeInterfaceEntity>> ENERGY_PIPE = BLOCK_ENTITY_TYPES.register("energy_pipe", () -> new BlockEntityType<>(EnergyPipeInterfaceEntity::new, BlockContent.ENERGY_PIPE_CONNECTION.get(), BlockContent.SUPERCONDUCTOR_CONNECTION.get(), BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get()));
    public static final Supplier<BlockEntityType<ItemPipeInterfaceEntity>> ITEM_PIPE = BLOCK_ENTITY_TYPES.register("item_pipe", () -> new BlockEntityType<>(ItemPipeInterfaceEntity::new, BlockContent.ITEM_PIPE_CONNECTION.get(), BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<ItemFilterBlockEntity>> ITEM_FILTER = BLOCK_ENTITY_TYPES.register("item_filter", () -> new BlockEntityType<>(ItemFilterBlockEntity::new, BlockContent.ITEM_FILTER.get()));
    @AssignSidedInventory
    public static final Supplier<BlockEntityType<SmartSplitterBlockEntity>> SMART_SPLITTER = BLOCK_ENTITY_TYPES.register("smart_splitter", () -> new BlockEntityType<>(SmartSplitterBlockEntity::new, BlockContent.SMART_SPLITTER.get()));

    @AssignSidedEnergy
    public static final Supplier<BlockEntityType<EnergyTransmissionPoleEntity>> ENERGY_TRANSMISSION_POLE = BLOCK_ENTITY_TYPES.register("energy_transmission_pole", () -> new BlockEntityType<>(EnergyTransmissionPoleEntity::new, BlockContent.ENERGY_TRANSMISSION_POLE.get()));

    public static final Supplier<BlockEntityType<AddonBlockEntity>> ADDON = BLOCK_ENTITY_TYPES.register("addon", () -> new BlockEntityType<>(AddonBlockEntity::new,
            BlockContent.MACHINE_SPEED_ADDON.get(),
            BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.get(),
            BlockContent.MACHINE_EFFICIENCY_ADDON.get(),
            BlockContent.SYNERGY_MATRIX_ADDON.get(),
            BlockContent.MACHINE_FLUID_ADDON.get(),
            BlockContent.MACHINE_HUNTER_ADDON.get(),
            BlockContent.MACHINE_YIELD_ADDON.get(),
            BlockContent.CROP_FILTER_ADDON.get(),
            BlockContent.MACHINE_EXTENDER.get(),
            BlockContent.MACHINE_CAPACITOR_ADDON.get(),
            BlockContent.POWER_BANK_ADDON_EXTENDER.get(),
            BlockContent.QUARRY_ADDON.get(),
            BlockContent.MACHINE_SILK_TOUCH_ADDON.get(),
            BlockContent.MACHINE_BURST_ADDON.get()
    ));

    @AssignSidedInventory
    @AssignSidedEnergy
    @AssignSidedFluid
    public static final Supplier<BlockEntityType<MachineCoreEntity>> MACHINE_CORE = BLOCK_ENTITY_TYPES.register("machine_core", () -> new BlockEntityType<>(MachineCoreEntity::new,
            BlockContent.MACHINE_CORE_1.get(),
            BlockContent.MACHINE_CORE_2.get(),
            BlockContent.MACHINE_CORE_3.get(),
            BlockContent.MACHINE_CORE_4.get(),
            BlockContent.MACHINE_CORE_5.get(),
            BlockContent.MACHINE_CORE_6.get(),
            BlockContent.MACHINE_CORE_7.get(),
            BlockContent.COMPLEX_PLATING.get()
    ));

    public static final Supplier<BlockEntityType<IndustrialDoorBlockEntity>> INDUSTRIAL_DOOR = BLOCK_ENTITY_TYPES.register("industrial_door", () -> new BlockEntityType<>(IndustrialDoorBlockEntity::new, BlockContent.INDUSTRIAL_DOOR.get()));
    public static final Supplier<BlockEntityType<HangarDoorBlockEntity>> HANGAR_DOOR = BLOCK_ENTITY_TYPES.register("hangar_door", () -> new BlockEntityType<>(HangarDoorBlockEntity::new, BlockContent.HANGAR_DOOR.get()));


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
