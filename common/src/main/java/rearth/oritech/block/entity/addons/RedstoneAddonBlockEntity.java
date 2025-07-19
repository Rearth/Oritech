package rearth.oritech.block.entity.addons;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
        NetworkManager.sendBlockHandle(this, new RedstoneAddonClientUpdate(pos, getControllerPos(), monitoredSlot, activeMode.ordinal(), currentOutput));
    }
    
    public void sendDataToServer() {
        NetworkManager.sendToServer(new RedstoneAddonServerUpdate(pos, getControllerPos(), monitoredSlot, activeMode.ordinal(), currentOutput));
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
        
        if (activeMode != RedstoneMode.INPUT_CONTROL) return;
        
        if (getCachedController() != null)
            cachedController.onRedstoneEvent(isPowered);
        
    }

    @Override
    public int getComparatorOutput() {
        return currentOutput;
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        sendDataToClient();
        buf.writeBlockPos(pos);
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
    
    public static void receiveOnServer(RedstoneAddonServerUpdate message, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        if (player.getWorld().getBlockEntity(message.position) instanceof RedstoneAddonBlockEntity addonEntity) {
            addonEntity.activeMode = RedstoneMode.values()[message.targetMode()];
            addonEntity.monitoredSlot = message.targetSlot();
        }
    }
    
    public static void receiveOnClient(RedstoneAddonClientUpdate message, World world, DynamicRegistryManager dynamicRegistryManager) {
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
    public record RedstoneAddonClientUpdate(BlockPos position, BlockPos controllerPos, int targetSlot, int targetMode, int currentOutput) implements CustomPayload {
        
        public static final CustomPayload.Id<RedstoneAddonClientUpdate> PACKET_ID = new CustomPayload.Id<>(Oritech.id("redstoneaddonclient"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
    
    public record RedstoneAddonServerUpdate(BlockPos position, BlockPos controllerPos, int targetSlot, int targetMode, int currentOutput) implements CustomPayload {
        
        public static final CustomPayload.Id<RedstoneAddonServerUpdate> PACKET_ID = new CustomPayload.Id<>(Oritech.id("redstoneaddonserver"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
}
