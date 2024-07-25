package rearth.oritech.block.entity.machines.addons;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.machines.addons.MachineAddonBlock;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.RedstoneAddonScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;

public class RedstoneAddonBlockEntity extends AddonBlockEntity implements BlockEntityTicker<RedstoneAddonBlockEntity>, ExtendedScreenHandlerFactory {
    
    private RedstoneControllable cachedController;
    public RedstoneMode activeMode = RedstoneMode.INPUT_CONTROL;
    public int monitoredSlot = 0;
    
    public int currentOutput;
    
    public RedstoneAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REDSTONE_ADDON_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, RedstoneAddonBlockEntity blockEntity) {
        if (world.isClient || !isConnected() || activeMode == RedstoneMode.INPUT_CONTROL) return;
        
        var lastOutput = currentOutput;
        
        switch (activeMode) {
            case OUTPUT_POWER -> currentOutput = cachedController.getComparatorEnergyAmount();
            case OUTPUT_SLOT -> currentOutput = cachedController.getComparatorSlotAmount(monitoredSlot);
            case OUTPUT_PROGRESS -> currentOutput = cachedController.getComparatorProgress();
            case OUTPUT_ACTIVE -> currentOutput = cachedController.getComparatorActiveState();
            case INPUT_CONTROL -> currentOutput = 0;
        }
        
        if (currentOutput != lastOutput) {
            this.markDirty();
        }
        
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("slot", monitoredSlot);
        nbt.putInt("mode", activeMode.ordinal());
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        monitoredSlot = nbt.getInt("slot");
        activeMode = RedstoneMode.values()[nbt.getInt("mode")];
    }
    
    public void sendDataToClient() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.RedstoneAddonSyncPacket(pos, getControllerPos(), monitoredSlot, activeMode.ordinal(), currentOutput));
    }
    
    public void sendDataToServer() {
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.RedstoneAddonSyncPacket(pos, getControllerPos(), monitoredSlot, activeMode.ordinal(), currentOutput));
    }
    
    private boolean isConnected() {
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    public RedstoneControllable getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        if (world.getBlockEntity(getControllerPos()) instanceof RedstoneControllable redstoneControllable) {
            cachedController = redstoneControllable;
        }
        
        return cachedController;
    }
    
    public void setRedstonePowered(boolean isPowered) {
        this.markDirty();
        
        if (getCachedController() != null)
            cachedController.onRedstoneEvent(isPowered);
        
    }
    
    @Override
    public Object getScreenOpeningData(ServerPlayerEntity player) {
        sendDataToClient();
        return new ModScreens.BasicData(pos);
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RedstoneAddonScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    public void handleClientBound(NetworkContent.RedstoneAddonSyncPacket message) {
        this.currentOutput = message.currentOutput();
        this.activeMode = RedstoneMode.values()[message.targetMode()];
        this.monitoredSlot = message.targetSlot();
        this.setControllerPos(message.controllerPos());
    }
    
    public void handleServerBound(NetworkContent.RedstoneAddonSyncPacket message) {
        this.activeMode = RedstoneMode.values()[message.targetMode()];
        this.monitoredSlot = message.targetSlot();
    }
    
    public enum RedstoneMode {
        OUTPUT_POWER, OUTPUT_SLOT, OUTPUT_PROGRESS, OUTPUT_ACTIVE, INPUT_CONTROL
    }
    
    public interface RedstoneControllable {
        int getComparatorEnergyAmount();
        int getComparatorSlotAmount(int slot);
        int getComparatorProgress();
        int getComparatorActiveState();
        void onRedstoneEvent(boolean isPowered);
    }
    
}
