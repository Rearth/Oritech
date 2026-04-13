package rearth.oritech.client.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleFluidStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.StackContext;

import java.util.*;

/**
 * Base screen handler for all Oritech machine screens.
 * <p>
 * Handles: machine inventory slots, player inventory slots, armor slots,
 * energy storage reference, fluid storage references, screen data.
 */
public class OritechScreenHandler extends AbstractContainerMenu implements MachineMenuHandler {
    
    @NotNull
    public final Inventory playerInventory;
    @NotNull
    public final Container inventory;
    @NotNull
    public final BlockPos blockPos;
    @NotNull
    public final ScreenProvider screenData;
    
    private final List<DisplayDataSource> dataDisplays = new ArrayList<>();
    public final List<FluidApi.SingleSlotStorage> fluidStorages;
    
    public BlockState machineBlock;
    public BlockEntity blockEntity;
    public List<Integer> armorSlots;
    
    public OritechScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public OritechScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(((ScreenProvider) blockEntity).getScreenHandlerType(), syncId);
        
        this.screenData = (ScreenProvider) blockEntity;
        this.blockPos = blockEntity.getBlockPos();
        this.playerInventory = playerInventory;
        this.machineBlock = blockEntity.getBlockState();
        this.blockEntity = blockEntity;
        this.inventory = screenData.getDisplayedInventory();
        
        if (this.inventory != null)
            this.inventory.startOpen(playerInventory.player);
        
        addEnergyDisplay();
        addProgressDisplay();
        addFluidDisplay();
        addAdditionalDisplays();
        
        fluidStorages = new ArrayList<>(screenData.getInteractableFluidStorages());
        
        assignFluidTankIndices();
        
        buildItemSlots();
    }
    
    public void addFluidDisplay() {
        if (blockEntity instanceof FluidApi.BlockProvider blockProvider) {
            var storage = blockProvider.getFluidStorage(null);
            if (storage instanceof SimpleFluidStorage singleSlotStorage) {
                var source = DisplayDataSource.CreateFluid(singleSlotStorage, screenData.getFluidConfiguration(), screenData);
                dataDisplays.add(source);
            }
        }
    }

    protected void addEnergyDisplay() {
        if (screenData.showEnergy() && blockEntity instanceof EnergyApi.BlockProvider energyProvider) {
            var storage = energyProvider.getEnergyStorage(null);
            dataDisplays.add(DisplayDataSource.CreateEnergy(storage, screenData.getEnergyConfiguration(), screenData));
        }
    }

    protected void addProgressDisplay() {
        if (screenData.showProgress()) {
            dataDisplays.add(DisplayDataSource.CreateProgress(screenData, blockEntity));
        }
    }

    protected void addAdditionalDisplays() {}
    
    public Collection<DisplayDataSource> getDataDisplays() {
        return dataDisplays;
    }
    
    /**
     * Matches each FluidDataSource to its index in the fluidStorages list
     * by comparing storage references. This enables the client to send
     * the correct tank index when clicking a fluid display widget.
     */
    private void assignFluidTankIndices() {
        for (var display : dataDisplays) {
            if (display instanceof DisplayDataSource.FluidDataSource fluidSource) {
                for (int i = 0; i < fluidStorages.size(); i++) {
                    if (fluidStorages.get(i) == fluidSource.getStorage()) {
                        fluidSource.setTankIndex(i);
                        break;
                    }
                }
            }
        }
    }
    
    private void buildItemSlots() {
        // Machine inventory slots
        for (var slot : screenData.getGuiSlots()) {
            addMachineSlot(slot.index(), slot.x(), slot.y(), slot.output());
        }
        
        // Player inventory (3 rows of 9, starting at x=8, y=84)
        addPlayerInventory(playerInventory, 8, 84 + (isTall() ? 20 : 0));
        
        // Armor slots (optional): 4 armor + 1 offhand
        if (screenData.showArmor()) {
            armorSlots = new ArrayList<>(5);
            for (int i = 0; i < playerInventory.armor.size() + 1; i++) {
                final var armorIndex = i;
                var slot = this.addSlot(new Slot(playerInventory, 36 + i, -20, i * 19) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        if (armorIndex == 4) return super.mayPlace(stack); // offhand slot
                        if (stack.getItem() instanceof ArmorItem armorItem) {
                            return super.mayPlace(stack) && armorItem.getEquipmentSlot().getIndex() == armorIndex;
                        }
                        return false;
                    }
                });
                armorSlots.add(slot.index);
            }
        }
    }
    
    public boolean isTall() {
        return false;
    }
    
    public void addMachineSlot(int inventorySlot, int x, int y, boolean output) {
        if (output) {
            this.addSlot(new BasicMachineOutputSlot(inventory, inventorySlot, x, y));
        } else {
            this.addSlot(new Slot(inventory, inventorySlot, x, y));
        }
    }
    
    /**
     * Adds the standard player inventory slots (27 main + 9 hotbar).
     */
    protected void addPlayerInventory(Inventory playerInventory, int startX, int startY) {
        // Main inventory (3 rows of 9, slot indices 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        // Hotbar (1 row of 9, slot indices 0-8)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        var slot = this.slots.get(invSlot);
        
        if (slot.hasItem()) {
            var originalStack = slot.getItem();
            var newStack = originalStack.copy();
            
            int machineSize = this.inventory.getContainerSize();
            int playerInvStart = getPlayerInvStartSlot(newStack);
            int playerInvEnd = getPlayerInvEndSlot(newStack);
            int totalSize = this.slots.size();
            
            // Machine → player
            if (invSlot < machineSize) {
                if (!this.moveItemStackTo(originalStack, playerInvStart, playerInvEnd, true))
                    return ItemStack.EMPTY;
            }
            // Player → machine
            else if (invSlot >= playerInvStart && invSlot < playerInvEnd) {
                if (!this.moveItemStackTo(originalStack, 0, machineSize, false))
                    return ItemStack.EMPTY;
            }
            // Armor/offhand → player
            else if (invSlot >= playerInvEnd && invSlot < totalSize) {
                if (!this.moveItemStackTo(originalStack, playerInvStart, playerInvEnd, true))
                    return ItemStack.EMPTY;
            }
            
            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            return newStack;
        }
        
        return ItemStack.EMPTY;
    }
    
    public int getPlayerInvStartSlot(ItemStack stack) {
        return this.inventory.getContainerSize();
    }
    
    public int getPlayerInvEndSlot(ItemStack stack) {
        return getPlayerInvStartSlot(stack) + 36;
    }
    
    public int getMachineInvStartSlot(ItemStack stack) {
        return 0;
    }
    
    public int getMachineInvEndSlot(ItemStack stack) {
        return this.inventory.getContainerSize();
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }
    
    @Override
    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    public boolean showRedstoneAddon() {
        return screenData.hasRedstoneControlAvailable();
    }
    
    @Override
    public void broadcastChanges() {
        if (blockEntity instanceof NetworkedBlockEntity networkedBlockEntity)
            networkedBlockEntity.sendUpdate(SyncType.GUI_TICK, (ServerPlayer) playerInventory.player);
        super.broadcastChanges();
    }
    
    public record FluidContainerInteractionPacket(BlockPos position, int tankIndex, boolean extract) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FluidContainerInteractionPacket> PACKET_ID =
            new CustomPacketPayload.Type<>(Oritech.id("fluid_container_interaction"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public static void handleFluidContainerInteraction(FluidContainerInteractionPacket packet, Player player, RegistryAccess registryAccess) {
        if (!(player.containerMenu instanceof OritechScreenHandler handler)) return;
        if (packet.tankIndex() < 0 || packet.tankIndex() >= handler.fluidStorages.size()) return;
        
        var carriedStack = player.containerMenu.getCarried();
        if (carriedStack.isEmpty()) return;
        
        var usedStack = carriedStack;
        if (carriedStack.getCount() > 1) {
            usedStack = carriedStack.copyWithCount(1);
        }
        
        var stackRef = new StackContext(usedStack, updated -> {
            if (carriedStack.getCount() > 1) {
                carriedStack.shrink(1);
                if (!player.getInventory().add(updated)) {
                    player.drop(updated, true);
                }
            } else {
                player.containerMenu.setCarried(updated);
            }
        });
        
        var itemFluidStorage = FluidApi.ITEM.find(stackRef);
        if (itemFluidStorage == null) return;
        
        var tankStorage = handler.fluidStorages.get(packet.tankIndex());
        
        if (packet.extract()) {
            // Right click: tank → item
            FluidApi.transferFirst(tankStorage, itemFluidStorage, Long.MAX_VALUE, false);
        } else {
            // Left click: item → tank
            FluidApi.transferFirst(itemFluidStorage, tankStorage, Long.MAX_VALUE, false);
        }
    }
    
    // endregion
}
