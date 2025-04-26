package rearth.oritech.network;

import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.MenuRegistry;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.energy.containers.DynamicStatisticEnergyStorage;
import rearth.oritech.api.energy.containers.SimpleEnergyStorage;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.block.base.entity.*;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.accelerator.BlackHoleBlockEntity;
import rearth.oritech.block.entity.accelerator.ParticleCollectorBlockEntity;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.block.entity.arcane.EnchanterBlockEntity;
import rearth.oritech.block.entity.arcane.EnchantmentCatalystBlockEntity;
import rearth.oritech.block.entity.arcane.SpawnerControllerBlockEntity;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.PlayerAugmentsClient;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.block.entity.interaction.*;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.block.entity.pipes.ItemPipeInterfaceEntity;
import rearth.oritech.block.entity.processing.CentrifugeBlockEntity;
import rearth.oritech.block.entity.reactor.ReactorAbsorberPortEntity;
import rearth.oritech.block.entity.reactor.ReactorControllerBlockEntity;
import rearth.oritech.block.entity.reactor.ReactorFuelPortEntity;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.item.tools.armor.BaseJetpackItem;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.MultiblockMachineController;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Map;

public class NetworkContent {
    
    public static final OwoNetChannel MACHINE_CHANNEL = OwoNetChannel.create(Oritech.id("machine_data"));
    public static final OwoNetChannel UI_CHANNEL = OwoNetChannel.create(Oritech.id("ui_interactions"));
    
    // Server -> Client
    public record MachineSyncPacket(BlockPos position, long energy, long maxEnergy, long maxInsert, long maxExtract,
                                    int progress,
                                    OritechRecipe activeRecipe, InventoryInputMode inputMode, long lastWorkedAt,
                                    boolean disabledViaRedstone) {
    }
    
    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) {
    }
    
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) {
    }
    
    public record RedstoneAddonSyncPacket(BlockPos position, BlockPos controllerPos, int targetSlot, int targetMode,
                                          int currentOutput) {
    }
    
    public record GeneratorUISyncPacket(BlockPos position, int burnTime, boolean steamAddon) {
    }
    
    public record MachineSetupEventPacket(BlockPos position) {
    }
    
    public record AcceleratorParticleRenderPacket(BlockPos position, List<Vec3d> particleTrail) {
    }
    
    public record AcceleratorParticleInsertEventPacket(BlockPos position) {
    }
    
    public record DroneCardEventPacket(BlockPos position, String message) {
    }
    
    public record ParticleAcceleratorAnimationPacket(BlockPos position) {
    }
    
    public record MachineFrameMovementPacket(BlockPos position, BlockPos currentTarget, BlockPos lastPosition,
                                             BlockPos areaMin, BlockPos areaMax, boolean redstoneDisable) {
    }   // times are in ticks
    
    public record QuarryTargetPacket(BlockPos position, BlockPos quarryTarget, int range, int yieldAddons,
                                     float operationSpeed) {
    }
    
    public record SteamEngineSyncPacket(BlockPos position, float speed, float efficiency, long energyProduced, long steamConsumed, int slaves) {}
    
    public record SpawnerSyncPacket(BlockPos position, Identifier spawnedMob, boolean hasCage, int collectedSouls,
                                    int maxSouls) {
    }
    
    public record MachineFrameGuiPacket(BlockPos position, long currentEnergy, long maxEnergy, int progress) {
    }
    
    // for use with addon providers to sync energy state
    public record GenericEnergySyncPacket(BlockPos position, long currentEnergy, long maxEnergy) {
    }
    
    public record FullEnergySyncPacket(BlockPos position, long currentEnergy, long maxEnergy, long maxInsert,
                                       long maxExtract) {
    }
    
    public record EnergyStatisticsPacket(BlockPos position, DynamicStatisticEnergyStorage.EnergyStatistics data) {
    }
    
    public record UnstableContainerContentPacket(BlockPos position, Identifier captured, float quality) {
    }
    
    public record ItemFilterSyncPacket(BlockPos position, ItemFilterBlockEntity.FilterData data) {
    }   // this goes both ways
    
    public record LaserArmSyncPacket(BlockPos position, BlockPos target, long lastFiredAt, int areaSize,
                                     int yieldAddons, int hunterAddons, int hunterTargetMode, boolean cropAddon,
                                     int targetEntityId, boolean redstonePowered) {
    }
    
    public record DeepDrillSyncPacket(BlockPos position, long lastWorkTime) {
    }
    
    public record SingleVariantFluidSyncPacketAPI(BlockPos position, String fluidType, long amount) {
    }
    
    public record ItemPipeVisualTransferPacket(BlockPos position, List<Long> codedStops, ItemStack moved) {
    }
    
    public record EnchanterSelectionPacket(BlockPos position, String enchantment) {
    }
    
    public record BlackHoleSuckPacket(BlockPos position, BlockPos from, long startedAt, long duration) {
    }
    
    public record EnchanterSyncPacket(BlockPos position, long energy, int progress, int maxProgress,
                                      int requiredCatalysts, int availableCatalysts) {
    }
    
    public record CatalystSyncPacket(BlockPos position, int storedSouls, int progress, boolean isHyperEnchanting,
                                     int maxSouls) {
    }
    
    public record GeneratorSteamSyncPacket(BlockPos position, long waterAmount, long steamAmount) {
    }
    
    public record DroneSendEventPacket(BlockPos position, boolean sendEvent, boolean receiveEvent) {
    
    }
    
    public record PumpWorkSyncPacket(BlockPos position, String fluidType, long workedAt) {
    }
    
    public record AugmentInstallTriggerPacket(BlockPos position, Identifier id, int operationId) {
    }
    
    public record LoadPlayerAugmentsToMachinePacket(BlockPos position) {
    }
    
    public record OpenAugmentScreenPacket(BlockPos position) {
    }
    
    public record AugmentPlayerTogglePacket(Identifier id) {
    }
    
    public record AugmentDataPacket(BlockPos position, List<Identifier> allResearched, List<Identifier> researchBlocks,
                                    List<Boolean> researchStates, List<Identifier> activeResearches,
                                    List<Long> startedTimes, List<Integer> researchTimes) {
    }
    
    public record AugmentOperationSyncPacket(Identifier id, int operation) {
    }
    
    public record CentrifugeFluidSyncPacket(BlockPos position, boolean fluidAddon, String fluidTypeIn, long amountIn,
                                            String fluidTypeOut,
                                            long amountOut) {
    }
    
    public record DronePortFluidSyncPacket(BlockPos position, boolean fluidAddon, String fluidType, long amount) {
    }
    
    public record JetpackUsageUpdatePacket(long energyStored, String fluidType, long fluidAmount) {
    }
    
    public record InventorySyncPacket(BlockPos position, List<ItemStack> heldStacks) {
    }
    
    public record ReactorUIDataPacket(BlockPos position, BlockPos min, BlockPos max, BlockPos previewMax) {
    }
    
    public record ReactorPortDataPacket(BlockPos position, int capacity, int remaining) {
    }
    
    public record ReactorUISyncPacket(BlockPos position, List<BlockPos> componentPositions,
                                      List<ReactorControllerBlockEntity.ComponentStatistics> componentHeats,
                                      long energy) {
    }
    
    @SuppressWarnings("unchecked")
    public static void registerChannels() {
        
        Oritech.LOGGER.debug("Registering oritech channels");
        
        MACHINE_CHANNEL.builder().register(ItemFilterBlockEntity.FILTER_ITEMS_ENDEC, (Class<Map<Integer, ItemStack>>) (Object) Map.class); // I don't even know what kind of abomination this cast is, but it seems to work
        MACHINE_CHANNEL.builder().register(OritechRecipeType.ORI_RECIPE_ENDEC, OritechRecipe.class);
        
        
        MACHINE_CHANNEL.registerClientbound(MachineSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof MachineBlockEntity machine) {
                machine.handleNetworkEntry(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(MachineSetupEventPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof MultiblockMachineController machine) {
                System.out.println("playing setup on client!");
                machine.playSetupAnimation();
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(EnchanterSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof EnchanterBlockEntity machine) {
                machine.handleSyncPacket(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(ItemFilterSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof ItemFilterBlockEntity filter) {
                filter.setFilterSettings(message.data);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(LaserArmSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof LaserArmBlockEntity laserArmBlock) {
                laserArmBlock.setCurrentTarget(message.target);
                laserArmBlock.setLastFiredAt(message.lastFiredAt);
                laserArmBlock.areaSize = message.areaSize;
                laserArmBlock.yieldAddons = message.yieldAddons;
                laserArmBlock.hunterAddons = message.hunterAddons;
                laserArmBlock.hasCropFilterAddon = message.cropAddon;
                laserArmBlock.setLivingTargetFromNetwork(message.targetEntityId);
                laserArmBlock.hunterTargetMode = LaserArmBlockEntity.HunterTargetMode.fromValue(message.hunterTargetMode);
                laserArmBlock.setRedstonePowered(message.redstonePowered);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(DeepDrillSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof DeepDrillEntity drillBlock) {
                drillBlock.setLastWorkTime(message.lastWorkTime);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(CatalystSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof EnchantmentCatalystBlockEntity catalystBlock) {
                catalystBlock.handleNetworkPacket(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(GenericEnergySyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof EnergyApi.BlockProvider energyProvider && energyProvider.getEnergyStorage(null) instanceof DynamicEnergyStorage storage) {
                storage.capacity = message.maxEnergy;
                storage.amount = message.currentEnergy;
            } else if (entity instanceof EnergyApi.BlockProvider energyProvider && energyProvider.getEnergyStorage(null) instanceof SimpleEnergyStorage storage) {
                storage.setAmount(message.currentEnergy);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(EnergyStatisticsPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof ExpandableEnergyStorageBlockEntity storageBlock) {
                storageBlock.currentStats = message.data;
            } else if (entity instanceof UnstableContainerBlockEntity storageBlock) {
                storageBlock.currentStats = message.data;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(UnstableContainerContentPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof UnstableContainerBlockEntity storageBlock) {
                storageBlock.capturedBlock = Registries.BLOCK.get(message.captured).getDefaultState();
                storageBlock.qualityMultiplier = message.quality == 0 ? 1 : message.quality;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(FullEnergySyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof EnergyApi.BlockProvider energyProvider && energyProvider.getEnergyStorage(null) instanceof DynamicEnergyStorage storage) {
                storage.capacity = message.maxEnergy;
                storage.amount = message.currentEnergy;
                storage.maxExtract = message.maxExtract;
                storage.maxInsert = message.maxInsert;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(DroneSendEventPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof DronePortEntity dronePort) {
                if (message.sendEvent) dronePort.playSendAnimation();
                if (message.receiveEvent) dronePort.playReceiveAnimation();
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(DroneCardEventPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof DronePortEntity dronePort) {
                dronePort.setStatusMessage(message.message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(ItemPipeVisualTransferPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof ItemPipeInterfaceEntity itemPipe) {
                itemPipe.handleVisualTransferPacket(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(SingleVariantFluidSyncPacketAPI.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof FluidApi.BlockProvider fluidEntity && fluidEntity.getFluidStorage(null) instanceof SimpleFluidStorage fluidContainer) {
                fluidContainer.setStack(FluidStack.create(Registries.FLUID.get(Identifier.of(message.fluidType)), message.amount));
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(SpawnerSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof SpawnerControllerBlockEntity spawnerEntity) {
                spawnerEntity.loadEntityFromIdentifier(message.spawnedMob);
                spawnerEntity.hasCage = message.hasCage;
                spawnerEntity.collectedSouls = message.collectedSouls;
                spawnerEntity.maxSouls = message.maxSouls;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(GeneratorSteamSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof SteamEngineEntity steamEngine) {  // water and steam amounts are mixed up in steam engine
                steamEngine.boilerStorage.setOutStack(FluidStack.create(Fluids.WATER, message.steamAmount));
                steamEngine.boilerStorage.setInStack(FluidStack.create(FluidContent.STILL_STEAM.get(), message.waterAmount));
            } else if (entity instanceof UpgradableGeneratorBlockEntity generatorBlock) {
                generatorBlock.boilerStorage.setInStack(FluidStack.create(Fluids.WATER, message.waterAmount));
                generatorBlock.boilerStorage.setOutStack(FluidStack.create(FluidContent.STILL_STEAM.get(), message.steamAmount));
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(AcceleratorParticleRenderPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
                acceleratorBlock.onReceiveMovement(message.particleTrail);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(AcceleratorControllerBlockEntity.LastEventPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position());
            
            if (entity instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
                acceleratorBlock.onReceivedEvent(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(AcceleratorParticleInsertEventPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position());
            
            if (entity instanceof AcceleratorControllerBlockEntity acceleratorBlock) {
                acceleratorBlock.onParticleInsertedClient();
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(PumpWorkSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof PumpBlockEntity pump) {
                var variant = Registries.FLUID.get(Identifier.of(message.fluidType));
                pump.setLastPumpedVariant(variant);
                pump.setLastPumpTime(message.workedAt);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(CentrifugeFluidSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof CentrifugeBlockEntity centrifuge) {
                centrifuge.hasFluidAddon = message.fluidAddon;
                var inStack = FluidStack.create(Registries.FLUID.get(Identifier.of(message.fluidTypeIn)), message.amountIn);
                var outStack = FluidStack.create(Registries.FLUID.get(Identifier.of(message.fluidTypeOut)), message.amountOut);
                centrifuge.fluidContainer.setInStack(inStack);
                centrifuge.fluidContainer.setOutStack(outStack);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(DronePortFluidSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof DronePortEntity dronePort) {
                dronePort.hasFluidAddon = message.fluidAddon;
                dronePort.fluidStorage.setStack(FluidStack.create(Registries.FLUID.get(Identifier.of(message.fluidType)), message.amount));
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(GeneratorUISyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof UpgradableGeneratorBlockEntity generatorBlock) {
                generatorBlock.setCurrentMaxBurnTime(message.burnTime);
                generatorBlock.isProducingSteam = message.steamAddon;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(BlackHoleSuckPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof BlackHoleBlockEntity hole) {
                hole.onClientPullEvent(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(MachineFrameMovementPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof FrameInteractionBlockEntity machine) {
                machine.setCurrentTarget(message.currentTarget);
                machine.setLastTarget(message.lastPosition);
                machine.setMoveStartedAt(access.player().getWorld().getTime());
                machine.setAreaMin(message.areaMin);
                machine.setAreaMax(message.areaMax);
                machine.disabledViaRedstone = message.redstoneDisable();
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(QuarryTargetPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof DestroyerBlockEntity machine) {
                machine.quarryTarget = message.quarryTarget;
                machine.range = message.range;
                machine.yieldAddons = message.yieldAddons;
                
                var oldData = machine.getBaseAddonData();
                var newData = new MachineAddonController.BaseAddonData(message.operationSpeed, oldData.efficiency(), oldData.energyBonusCapacity(), oldData.energyBonusTransfer(), oldData.extraChambers());
                machine.setBaseAddonData(newData);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(SteamEngineSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof SteamEngineEntity machine) {
                machine.clientStats = message;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(ParticleAcceleratorAnimationPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof ParticleCollectorBlockEntity machine) {
                machine.playAnimation();
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(InventorySyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof ScreenProvider machine) {
                List<ItemStack> heldStacks = message.heldStacks;
                for (int i = 0; i < heldStacks.size(); i++) {
                    var stack = heldStacks.get(i);
                    machine.getDisplayedInventory().setStack(i, stack);
                }
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(MachineFrameGuiPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof ItemEnergyFrameInteractionBlockEntity machine) {
                machine.setCurrentProgress(message.progress);
                var energyStorage = machine.getEnergyStorage();
                energyStorage.amount = message.currentEnergy;
                energyStorage.capacity = message.maxEnergy;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(RedstoneAddonSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof RedstoneAddonBlockEntity machine) {
                machine.handleClientBound(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(EnchanterSelectionPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof EnchanterBlockEntity enchanter) {
                enchanter.handleEnchantmentSelection(message);
            }
            
        }));
        
        
        MACHINE_CHANNEL.registerClientbound(AugmentDataPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof AugmentApplicationEntity enhancer) {
                enhancer.handleAugmentUpdatePacket(message);
            }
            
        }));
        
        
        MACHINE_CHANNEL.registerClientbound(AugmentOperationSyncPacket.class, ((message, access) -> {
            if (access != null)
                PlayerAugmentsClient.handlePlayerAugmentOperation(message, access);   // this weird redict is need for server-only class-loading reasons?
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(ReactorUIDataPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof ReactorControllerBlockEntity reactor) {
                reactor.uiData = message;
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(ReactorUISyncPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof ReactorControllerBlockEntity reactor) {
                reactor.uiSyncData = message;
                reactor.energyStorage.setAmount(message.energy);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(ReactorPortDataPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            // this is what happens when you're too lazy to add an interface
            if (entity instanceof ReactorFuelPortEntity port) {
                port.currentFuelOriginalCapacity = message.capacity;
                port.availableFuel = message.remaining;
            } else if (entity instanceof ReactorAbsorberPortEntity port) {
                port.currentFuelOriginalCapacity = message.capacity;
                port.availableFuel = message.remaining;
            }
            
        }));
        
        UI_CHANNEL.registerServerbound(RedstoneAddonSyncPacket.class, (message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            if (entity instanceof RedstoneAddonBlockEntity machine) {
                machine.handleServerBound(message);
            }
            
        });
        
        UI_CHANNEL.registerServerbound(InventoryInputModeSelectorPacket.class, (message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof MachineBlockEntity machine) {
                machine.cycleInputMode();
            }
            
        });
        
        UI_CHANNEL.registerServerbound(InventoryProxySlotSelectorPacket.class, (message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof InventoryProxyAddonBlockEntity machine) {
                machine.setTargetSlot(message.slot);
            }
            
        });
        
        UI_CHANNEL.registerServerbound(ItemFilterSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof ItemFilterBlockEntity filter) {
                filter.setFilterSettings(message.data);
            }
            
        }));
        
        UI_CHANNEL.registerServerbound(EnchanterSelectionPacket.class, (message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof EnchanterBlockEntity enchanter) {
                enchanter.handleEnchantmentSelection(message);
            }
            
        });
        
        UI_CHANNEL.registerServerbound(JetpackUsageUpdatePacket.class, (message, access) -> {
            var player = access.player();
            var stack = player.getEquippedStack(EquipmentSlot.CHEST);
            if (!(stack.getItem() instanceof BaseJetpackItem)) return;
            
            // to prevent dedicated servers from kicking the player for flying
            player.networkHandler.floatingTicks = 0;
            
            stack.set(EnergyApi.ITEM.getEnergyComponent(), message.energyStored);
            if (message.fluidAmount > 0)
                stack.set(ComponentContent.STORED_FLUID.get(), FluidStack.create(Registries.FLUID.get(Identifier.of(message.fluidType)), message.fluidAmount));
            
        });
        
        UI_CHANNEL.registerServerbound(AugmentInstallTriggerPacket.class, (message, access) -> {
            var player = access.player();
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof AugmentApplicationEntity modifierEntity) {
                var operation = PlayerAugments.AugmentOperation.values()[message.operationId];
                switch (operation) {
                    case RESEARCH -> {
                        modifierEntity.researchAugment(message.id, player.isCreative(), player);
                    }
                    case ADD -> {
                        modifierEntity.installAugmentToPlayer(message.id, player);
                    }
                    case REMOVE -> {
                        modifierEntity.removeAugmentFromPlayer(message.id, player);
                    }
                }
            }
        });
        
        UI_CHANNEL.registerServerbound(LoadPlayerAugmentsToMachinePacket.class, (message, access) -> {
            var player = access.player();
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof AugmentApplicationEntity modifierEntity) {
                modifierEntity.loadResearchesFromPlayer(player);
            }
        });
        
        UI_CHANNEL.registerServerbound(OpenAugmentScreenPacket.class, (message, access) -> {
            var player = access.player();
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof AugmentApplicationEntity modifierEntity) {
                modifierEntity.screenInvOverride = true;
                MenuRegistry.openExtendedMenu(player, modifierEntity);
            }
        });
        
        UI_CHANNEL.registerServerbound(AugmentPlayerTogglePacket.class, (message, access) -> {
            var player = access.player();
            AugmentApplicationEntity.toggleAugmentForPlayer(message.id, player);
        });
        
    }
    
}
