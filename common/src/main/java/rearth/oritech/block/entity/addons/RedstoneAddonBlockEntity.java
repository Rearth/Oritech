package rearth.oritech.block.entity.addons;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.client.ui.RedstoneAddonScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ComparatorOutputProvider;

public class RedstoneAddonBlockEntity extends AddonBlockEntity implements BlockEntityTicker<RedstoneAddonBlockEntity>, ExtendedMenuProvider, ComparatorOutputProvider {
    
    private RedstoneControllable cachedController;
    public RedstoneMode activeMode = RedstoneMode.INPUT_CONTROL;
    public int monitoredSlot = 0;
    
    public int currentOutput;
    
    public RedstoneAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REDSTONE_ADDON_ENTITY, pos, state);
    }
    
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, RedstoneAddonBlockEntity blockEntity) {
        if (world.isClientSide || !isConnected() || activeMode == RedstoneMode.INPUT_CONTROL) return;
        
        var lastOutput = currentOutput;
        
        switch (activeMode) {
            case OUTPUT_POWER -> currentOutput = cachedController.getComparatorEnergyAmount();
            case OUTPUT_SLOT -> currentOutput = cachedController.getComparatorSlotAmount(monitoredSlot);
            case OUTPUT_PROGRESS -> currentOutput = cachedController.getComparatorProgress();
            case OUTPUT_ACTIVE -> currentOutput = cachedController.getComparatorActiveState();
            case INPUT_CONTROL -> currentOutput = 0;
        }
        
        if (currentOutput != lastOutput) {
            this.setChanged();
        }
        
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putInt("slot", monitoredSlot);
        nbt.putInt("mode", activeMode.ordinal());
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        monitoredSlot = nbt.getInt("slot");
        activeMode = RedstoneMode.values()[nbt.getInt("mode")];
    }
    
    public void sendDataToClient() {
        NetworkManager.sendBlockHandle(this, new RedstoneAddonClientUpdate(worldPosition, getControllerPos(), monitoredSlot, activeMode.ordinal(), currentOutput));
    }
    
    public void sendDataToServer() {
        NetworkManager.sendToServer(new RedstoneAddonServerUpdate(worldPosition, getControllerPos(), monitoredSlot, activeMode.ordinal(), currentOutput));
    }
    
    private boolean isConnected() {
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    public RedstoneControllable getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        if (level.getBlockEntity(getControllerPos()) instanceof RedstoneControllable redstoneControllable) {
            cachedController = redstoneControllable;
        }
        
        return cachedController;
    }
    
    public void setRedstonePowered(boolean isPowered) {
        this.setChanged();
        
        if (activeMode != RedstoneMode.INPUT_CONTROL) return;
        
        if (getCachedController() != null)
            cachedController.onRedstoneEvent(isPowered);
        
    }

    @Override
    public int getComparatorOutput() {
        return currentOutput;
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        sendDataToClient();
        buf.writeBlockPos(worldPosition);
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new RedstoneAddonScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    public static void receiveOnServer(RedstoneAddonServerUpdate message, Player player, RegistryAccess dynamicRegistryManager) {
        if (player.level().getBlockEntity(message.position) instanceof RedstoneAddonBlockEntity addonEntity) {
            addonEntity.activeMode = RedstoneMode.values()[message.targetMode()];
            addonEntity.monitoredSlot = message.targetSlot();
        }
    }
    
    public static void receiveOnClient(RedstoneAddonClientUpdate message, Level world, RegistryAccess dynamicRegistryManager) {
        if (world.getBlockEntity(message.position) instanceof RedstoneAddonBlockEntity addonEntity) {
            addonEntity.currentOutput = message.currentOutput();
            addonEntity.activeMode = RedstoneMode.values()[message.targetMode()];
            addonEntity.monitoredSlot = message.targetSlot();
            addonEntity.setControllerPos(message.controllerPos());
        }
    }
    
    public enum RedstoneMode {
        OUTPUT_POWER, OUTPUT_SLOT, OUTPUT_PROGRESS, OUTPUT_ACTIVE, INPUT_CONTROL
    }
    
    public interface RedstoneControllable extends ComparatorOutputProvider {
        int getComparatorEnergyAmount();
        int getComparatorSlotAmount(int slot);
        int getComparatorProgress();
        int getComparatorActiveState();
        void onRedstoneEvent(boolean isPowered);

        /**
         * A redstone controllable machine only outputs a readable comparator signal from the controller addon block.
         * @return 0
         */
        @Override
        default int getComparatorOutput() {
            return 0;
        }
    }
    
    // we need 2 here because Neoforge is annoying as always and doesnt let me register it in both directions
    public record RedstoneAddonClientUpdate(BlockPos position, BlockPos controllerPos, int targetSlot, int targetMode, int currentOutput) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<RedstoneAddonClientUpdate> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("redstoneaddonclient"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record RedstoneAddonServerUpdate(BlockPos position, BlockPos controllerPos, int targetSlot, int targetMode, int currentOutput) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<RedstoneAddonServerUpdate> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("redstoneaddonserver"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
