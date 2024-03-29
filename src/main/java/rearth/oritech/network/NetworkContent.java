package rearth.oritech.network;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.machines.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.machines.generators.BigSolarPanelEntity;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.block.entity.machines.processing.CentrifugeBlockEntity;
import rearth.oritech.block.entity.pipes.ItemFilterBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.FluidProvider;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Map;

public class NetworkContent {
    
    public static final OwoNetChannel MACHINE_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "machine_data"));
    public static final OwoNetChannel UI_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "ui_interactions"));
    
    // Server -> Client
    public record MachineSyncPacket(BlockPos position, long energy, long maxEnergy, long maxInsert, int progress,
                                    OritechRecipe activeRecipe, InventoryInputMode inputMode) {
    }
    
    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) {
    }
    
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) {
    }
    
    public record GeneratorUISyncPacket(BlockPos position, int burnTime) {
    }
    
    public record MachineSetupEventPacket(BlockPos position) {
    }
    
    public record MachineFrameMovementPacket(BlockPos position, BlockPos currentTarget, BlockPos lastTarget,
                                             BlockPos areaMin, BlockPos areaMax) {
    }   // times are in ticks
    
    public record MachineFrameGuiPacket(BlockPos position, long currentEnergy, long maxEnergy, int progress) {
    }
    
    public record ItemFilterSyncPacket(BlockPos position, ItemFilterBlockEntity.FilterData data) {
    }   // this goes both ways
    
    public record LaserArmSyncPacket(BlockPos position, BlockPos target, long lastFiredAt) {
    }
    
    public record SingleVariantFluidSyncPacket(BlockPos position, String fluidType, long amount) {
    }
    
    public record CentrifugeFluidSyncPacket(BlockPos position, boolean fluidAddon, String fluidTypeIn, long amountIn, String fluidTypeOut,
                                            long amountOut) {
    }
    
    public record InventorySyncPacket(BlockPos position, List<ItemStack> heldStacks) {
    }
    
    @SuppressWarnings("unchecked")
    public static void registerChannels() {
        
        Oritech.LOGGER.info("Registering oritech channels");
        
        ReflectiveEndecBuilder.register(OritechRecipeType.ORI_RECIPE_ENDEC, OritechRecipe.class);
        ReflectiveEndecBuilder.register(ItemFilterBlockEntity.FILTER_ITEMS_ENDEC, (Class<Map<Integer, ItemStack>>) (Object) Map.class); // I don't even know what kind of abomination this cast is, but it seems to work
        
        MACHINE_CHANNEL.registerClientbound(MachineSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof MachineBlockEntity machine) {
                machine.handleNetworkEntry(message);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(MachineSetupEventPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof MachineBlockEntity machine) {
                machine.playSetupAnimation();
            } else if (entity instanceof LaserArmBlockEntity laserArmBlock) {
                laserArmBlock.playSetupAnimation();
            } else if (entity instanceof BigSolarPanelEntity solarPanel) {
                solarPanel.playSetupAnimation();
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
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(SingleVariantFluidSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof FluidProvider fluidProvider && fluidProvider.getForDirectFluidAccess() != null) {
                var storage = fluidProvider.getForDirectFluidAccess();
                storage.amount = message.amount;
                storage.variant = FluidVariant.of(Registries.FLUID.get(new Identifier(message.fluidType)));
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(CentrifugeFluidSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof CentrifugeBlockEntity centrifuge) {
                centrifuge.hasFluidAddon = message.fluidAddon;
                var inStorage = centrifuge.inputStorage;
                var outStorage = centrifuge.outputStorage;
                inStorage.amount = message.amountIn;
                outStorage.amount = message.amountOut;
                inStorage.variant = FluidVariant.of(Registries.FLUID.get(new Identifier(message.fluidTypeIn)));
                outStorage.variant = FluidVariant.of(Registries.FLUID.get(new Identifier(message.fluidTypeOut)));
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(GeneratorUISyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof UpgradableGeneratorBlockEntity generatorBlock) {
                generatorBlock.setCurrentMaxBurnTime(message.burnTime);
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(MachineFrameMovementPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            if (entity instanceof FrameInteractionBlockEntity machine) {
                machine.setCurrentTarget(message.currentTarget);
                machine.setLastTarget(message.lastTarget);
                machine.setMoveStartedAt(access.player().getWorld().getTime());
                machine.setAreaMin(message.areaMin);
                machine.setAreaMax(message.areaMax);
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
                System.out.println(message.slot);
            }
            
        });
        
        UI_CHANNEL.registerServerbound(ItemFilterSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof ItemFilterBlockEntity filter) {
                filter.setFilterSettings(message.data);
            }
            
        }));
        
    }
    
}
