package rearth.oritech.block.entity.reactor;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import io.netty.buffer.Unpooled;
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
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReactorFuelPortEntity extends BlockEntity implements ExtendedMenuProvider, ScreenProvider, ItemApi.BlockProvider {
    
    private final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::setChanged, new InventorySlotAssignment(0, 1, 1, 0));
    
    @SyncField(SyncType.GUI_TICK)
    public int availableFuel;
    @SyncField(SyncType.GUI_TICK)
    public int currentFuelOriginalCapacity;
    
    public ReactorFuelPortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_FUEL_PORT_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        
        nbt.putInt("available", availableFuel);
        nbt.putInt("capacity", currentFuelOriginalCapacity);
        
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        
        availableFuel = nbt.getInt("available");
        currentFuelOriginalCapacity = nbt.getInt("capacity");
        
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
    }
    
    // consumes remaining internal fuel when disabled, but will not consume new input items
    public boolean tryConsumeFuel(int amount, boolean disabled) {
        if (availableFuel >= amount) {
            availableFuel -= amount;
            return true;
        }
        
        if (disabled) return false;
        
        // try consume input
        var inputStack = inventory.getItem(0);
        if (inputStack.isEmpty()) return false;
        
        var craftingInv = new SimpleCraftingInventory(inputStack);
        var recipeCandidate = level.getRecipeManager().getRecipeFor(RecipeContent.REACTOR, craftingInv, level);
        
        if (recipeCandidate.isEmpty()) return false;
        
        var capacity = recipeCandidate.get().value().getTime();
        currentFuelOriginalCapacity = capacity;
        availableFuel = capacity - amount;
        inputStack.shrink(1);
        playLoadingSound();
        return true;
        
    }
    
    private void playLoadingSound() {
        var variation = level.random.nextFloat() * 0.6f - 0.2f;
        level.playSound(null, worldPosition, SoundContent.REACTOR_LOADING, SoundSource.BLOCKS, 0.5f, 0.8f + variation);
    }
    
    public void updateNetwork() {
        var usedBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        var fieldCount = NetworkManager.encodeFields(this, SyncType.GUI_TICK, usedBuf, level);
        if (fieldCount == 0) return;
        NetworkManager.sendBlockHandle(this, new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), SyncType.GUI_TICK, usedBuf.array()));
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
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
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
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
