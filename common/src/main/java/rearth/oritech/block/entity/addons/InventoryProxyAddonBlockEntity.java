package rearth.oritech.block.entity.addons;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.DelegatingInventoryStorage;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.client.ui.InventoryProxyScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

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
        
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;
        
        var controllerEntity = getCachedController();
        if (!(controllerEntity instanceof ItemApi.BlockProvider itemProvider)) return null;
        return itemProvider.getInventoryStorage(null);
    }
    
    private boolean isConnected() {
        var isUsed = this.getCachedState().get(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }
    
    private MachineAddonController getCachedController() {
        
        if (cachedController != null)
            return cachedController;
        
        cachedController = (MachineAddonController) Objects.requireNonNull(world).getBlockEntity(getControllerPos());
        return cachedController;
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        var data = new InventoryProxyScreenHandler.InvProxyData(pos, getControllerPos(), targetSlot);
        InventoryProxyScreenHandler.InvProxyData.PACKET_CODEC.encode(buf, data);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.translatable("title.oritech.inventory_proxy");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new InventoryProxyScreenHandler(syncId, playerInventory, this, getCachedController().getScreenProvider(), targetSlot);
    }
    
    public void setTargetSlot(int targetSlot) {
        this.targetSlot = targetSlot;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("target_slot", targetSlot);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        targetSlot = nbt.getInt("target_slot");
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    public static void receiveSlotSelection(InventoryProxySlotSelectorPacket packet, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        if (player.getWorld().getBlockEntity(packet.position) instanceof InventoryProxyAddonBlockEntity addonBlock)
            addonBlock.setTargetSlot(packet.slot);
    }
    
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) implements CustomPayload {
        
        public static final CustomPayload.Id<InventoryProxySlotSelectorPacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("proxy_slot_sel"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
}
