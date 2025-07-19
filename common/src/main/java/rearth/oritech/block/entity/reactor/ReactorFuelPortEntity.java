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
import net.minecraft.sound.SoundCategory;
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
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.InventorySlotAssignment;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.SimpleCraftingInventory;

import java.util.List;

public class ReactorFuelPortEntity extends BlockEntity implements ExtendedMenuProvider, ScreenProvider, ItemApi.BlockProvider {
    
    private final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::markDirty, new InventorySlotAssignment(0, 1, 1, 0));
    
    @SyncField(SyncType.GUI_TICK)
    public int availableFuel;
    @SyncField(SyncType.GUI_TICK)
    public int currentFuelOriginalCapacity;
    
    public ReactorFuelPortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_FUEL_PORT_BLOCK_ENTITY, pos, state);
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
    
    // consumes remaining internal fuel when disabled, but will not consume new input items
    public boolean tryConsumeFuel(int amount, boolean disabled) {
        if (availableFuel >= amount) {
            availableFuel -= amount;
            return true;
        }
        
        if (disabled) return false;
        
        // try consume input
        var inputStack = inventory.getStack(0);
        if (inputStack.isEmpty()) return false;
        
        var craftingInv = new SimpleCraftingInventory(inputStack);
        var recipeCandidate = world.getRecipeManager().getFirstMatch(RecipeContent.REACTOR, craftingInv, world);
        
        if (recipeCandidate.isEmpty()) return false;
        
        var capacity = recipeCandidate.get().value().getTime();
        currentFuelOriginalCapacity = capacity;
        availableFuel = capacity - amount;
        inputStack.decrement(1);
        playLoadingSound();
        return true;
        
    }
    
    private void playLoadingSound() {
        var variation = world.random.nextFloat() * 0.6f - 0.2f;
        world.playSound(null, pos, SoundContent.REACTOR_LOADING, SoundCategory.BLOCKS, 0.5f, 0.8f + variation);
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
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 55, 35));
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
