package rearth.oritech.block.entity.addons;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.DelegatingInventoryStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.client.ui.InventoryProxyScreenHandler;
import rearth.oritech.client.ui.InventoryProxyScreenHandler.InvProxyData;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class InventoryProxyAddonBlockEntity extends AddonBlockEntity implements ItemApi.BlockProvider, ExtendedMenuProvider {
    
    private MachineAddonController cachedController;
    private int targetSlot = 0;
    
    private final DelegatingInventoryStorage inventory = new DelegatingInventoryStorage(this::getTargetItemStorage, this::isConnected) {
        
        @Override
        public int insert(ItemStack inserted, boolean simulate) {
            return insertToSlot(inserted, targetSlot, simulate);
        }
        
        @Override
        public int extract(ItemStack extracted, boolean simulate) {
            return extractFromSlot(extracted, targetSlot, simulate);
        }
        
        @Override
        public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
            if (slot != targetSlot) return 0;
            return super.insertToSlot(inserted, slot, simulate);
        }
        
        @Override
        public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
            if (slot != targetSlot) return 0;
            return super.extractFromSlot(extracted, slot, simulate);
        }
    };
    
    public InventoryProxyAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.INVENTORY_PROXY_ADDON_ENTITY, pos, state);
    }
    
    private ItemApi.InventoryStorage getTargetItemStorage() {
        
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof ItemApi.BlockProvider itemProvider)) return null;
        return itemProvider.getInventoryStorage(null);
    }
    
    private boolean isConnected() {
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private MachineAddonController getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        cachedController = (MachineAddonController) Objects.requireNonNull(level).getBlockEntity(getControllerPos());
        return cachedController;
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        var data = new InventoryProxyScreenHandler.InvProxyData(worldPosition, getControllerPos(), targetSlot);
        InventoryProxyScreenHandler.InvProxyData.PACKET_CODEC.encode(buf, data);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("title.oritech.inventory_proxy");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new InventoryProxyScreenHandler(syncId, playerInventory, this, getCachedController().getScreenProvider(), targetSlot);
    }
    
    public void setTargetSlot(int targetSlot) {
        this.targetSlot = targetSlot;
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        nbt.putInt("target_slot", targetSlot);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        targetSlot = nbt.getInt("target_slot");
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    public static void receiveSlotSelection(InventoryProxySlotSelectorPacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        if (player.level().getBlockEntity(packet.position) instanceof InventoryProxyAddonBlockEntity addonBlock)
            addonBlock.setTargetSlot(packet.slot);
    }
    
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<InventoryProxySlotSelectorPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("proxy_slot_sel"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
