package rearth.oritech.block.entity.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.transfer.item.DelegatingInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.client.ui.InventoryProxyScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class InventoryProxyAddonBlockEntity extends AddonBlockEntity implements ItemProvider, MenuProvider {
    
    private MachineAddonController cachedController;
    private int targetSlot = 0;
    
    private final DelegatingInventoryStorage inventory = new DelegatingInventoryStorage(this::getTargetItemStorage, this::isConnected) {
        
        // forward non-indexed variants to indexed variants to skip the loop (performance optimization only)
        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return insert(targetSlot, resource, amount, transaction);
        }
        
        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return extract(targetSlot, resource, amount, transaction);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != targetSlot) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index != targetSlot) return 0;
            return super.extract(index, resource, amount, transaction);
        }
    };
    
    public InventoryProxyAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.INVENTORY_PROXY_ADDON_ENTITY.get(), pos, state);
    }
    
    private ItemStacksResourceHandler getTargetItemStorage() {
        
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (controllerEntity.getInventoryForAddon() instanceof ItemStacksResourceHandler storage) return storage;
        if (controllerEntity instanceof ItemProvider itemProvider && itemProvider.getItemLookup(null) instanceof ItemStacksResourceHandler storage) return storage;
        return null;
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
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        var data = new InventoryProxyScreenHandler.InvProxyData(worldPosition, getControllerPos(), targetSlot);
        InventoryProxyScreenHandler.InvProxyData.PACKET_CODEC.encode(buffer, data);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("target_slot", targetSlot);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        targetSlot = input.getIntOr("target_slot", 0);
    }
    
    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory;
    }
    
    public static void receiveSlotSelection(InventoryProxySlotSelectorPacket packet, IPayloadContext context) {
        if (context.player().level().getBlockEntity(packet.position) instanceof InventoryProxyAddonBlockEntity addonBlock)
            addonBlock.setTargetSlot(packet.slot);
    }
    
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) implements CustomPacketPayload {
        
        public static final Type<InventoryProxySlotSelectorPacket> PACKET_ID = new Type<>(Oritech.id("proxy_slot_sel"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
