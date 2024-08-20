package rearth.oritech.block.entity.arcane;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.EnchanterScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.*;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

public class EnchanterBlockEntity extends BlockEntity
    implements InventoryProvider, EnergyProvider, ScreenProvider, BlockEntityTicker<EnchanterBlockEntity>, ExtendedScreenHandlerFactory<ModScreens.BasicData> {
    
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(50000, 1000, 0) {
        @Override
        public void onFinalCommit() {
            super.onFinalCommit();
            EnchanterBlockEntity.this.markDirty();
        }
    };
    
    public final SimpleInventory inventory = new SimpleInventory(1) {
        @Override
        public void markDirty() {
            EnchanterBlockEntity.this.markDirty();
        }
    };
    protected final InventoryStorage inventoryStorage = InventoryStorage.of(inventory, null);
    
    public EnchanterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENCHANTER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, EnchanterBlockEntity blockEntity) {
    
    }
    
    @Override
    public ModScreens.BasicData getScreenOpeningData(ServerPlayerEntity player) {
        return new ModScreens.BasicData(pos);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnchanterScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public EnergyStorage getStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 26));
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return 128; // todo config parameter
    }
    
    @Override
    public float getProgress() {
        // todo
        return 0;
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
        return ModScreens.ENCHANTER_SCREEN;
    }
    
    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }
    
    @Override
    public InventoryStorage getInventory(Direction direction) {
        return inventoryStorage;
    }
}
