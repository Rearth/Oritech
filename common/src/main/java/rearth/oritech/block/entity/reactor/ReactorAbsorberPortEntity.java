package rearth.oritech.block.entity.reactor;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.InOutInventoryStorage;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class ReactorAbsorberPortEntity extends BlockEntity implements ExtendedMenuProvider, ScreenProvider, ItemApi.BlockProvider {
    
    private final InOutInventoryStorage inventory = new InOutInventoryStorage(1, this::markDirty, new InventorySlotAssignment(0, 1, 1, 0));
    
    @SyncField(SyncType.GUI_TICK)
    public int availableFuel;
    @SyncField(SyncType.GUI_TICK)
    public int currentFuelOriginalCapacity;
    
    public ReactorAbsorberPortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_ABSORBER_PORT_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        
        nbt.putInt("available", availableFuel);
        nbt.putInt("capacity", currentFuelOriginalCapacity);
        
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        
        availableFuel = nbt.getInt("available");
        currentFuelOriginalCapacity = nbt.getInt("capacity");
        
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
    }
    
    public int getAvailableFuel() {
        if (availableFuel > 0) {
            return availableFuel;
        }
        
        // try consume item
        var inputStack = inventory.getStack(0);
        if (inputStack.isEmpty()) return 0;
        
        if (inputStack.isIn(TagContent.REACTOR_COOLANT)) {
            var capacity = 1000;
            currentFuelOriginalCapacity = capacity;
            availableFuel = capacity;
            inputStack.decrement(1);
            onFuelConsumed();
        }
        
        return availableFuel;
    }
    
    public void consumeFuel(int amount) {
        if (availableFuel >= amount) {
            availableFuel -= amount;
            
            if (world.getTime() % 5 == 0)
                ParticleContent.COOLER_WORKING.spawn(world, pos.toCenterPos().add(0, 0.5, 0), 1);
        }
        
    }
    
    private void onFuelConsumed() {
        ParticleContent.COOLER_WORKING.spawn(world, pos.toCenterPos().add(0, 0.5, 0), 15);
    }
    
    public void updateNetwork() {
        var usedBuf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
        var fieldCount = NetworkManager.encodeFields(this, SyncType.GUI_TICK, usedBuf, world);
        if (fieldCount == 0) return;
        NetworkManager.sendBlockHandle(this, new NetworkManager.MessagePayload(pos, Registries.BLOCK_ENTITY_TYPE.getId(getType()), SyncType.GUI_TICK, usedBuf.array()));
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.of("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        return List.of(new ScreenProvider.GuiSlot(0, 80, 35));
    }
    
    @Override
    public boolean showEnergy() {
        return false;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }
    
    @Override
    public float getProgress() {
        return 0;
    }
    
    @Override
    public boolean showProgress() {
        return false;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.FUEL_PORT_SCREEN;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public boolean showExpansionPanel() {
        return false;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
}
