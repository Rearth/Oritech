package rearth.oritech.network;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.FrameInteractionBlockEntity;
import rearth.oritech.block.blocks.machines.interaction.LaserArmBlock;
import rearth.oritech.block.entity.machines.addons.InventoryProxyAddonBlockEntity;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class NetworkContent {

    public static final OwoNetChannel MACHINE_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "machine_data"));
    public static final OwoNetChannel UI_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "ui_interactions"));

    // Server -> Client
    public record MachineSyncPacket(BlockPos position, long energy, long maxEnergy, long maxInsert, int progress, OritechRecipe activeRecipe, InventoryInputMode inputMode) {}

    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) {}
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) {}
    public record MachineSetupEventPacket(BlockPos position) {}
    public record MachineFrameMovementPacket(BlockPos position, BlockPos currentTarget, BlockPos lastTarget, BlockPos areaMin, BlockPos areaMax) {};   // times are in ticks
    public record MachineFrameGuiPacket(BlockPos position, long currentEnergy, long maxEnergy, int progress){};
    public record LaserArmSyncPacket(BlockPos position, BlockPos target){};
    public record InventorySyncPacket(BlockPos position, List<ItemStack> heldStacks) {}
    
    public static void registerChannels() {

        Oritech.LOGGER.info("Registering oritech channels");

        ReflectiveEndecBuilder.register(OritechRecipeType.ORI_RECIPE_ENDEC, OritechRecipe.class);

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
            }
            
        }));
        
        MACHINE_CHANNEL.registerClientbound(LaserArmSyncPacket.class, ((message, access) -> {
            
            var entity = access.player().clientWorld.getBlockEntity(message.position);
            
            if (entity instanceof LaserArmBlockEntity laserArmBlock) {
                laserArmBlock.setTarget(message.target);
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

    }

}
