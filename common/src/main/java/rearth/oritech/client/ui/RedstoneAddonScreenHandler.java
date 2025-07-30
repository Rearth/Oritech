package rearth.oritech.client.ui;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RedstoneAddonScreenHandler extends AbstractContainerMenu {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final RedstoneAddonBlockEntity blockEntity;
    
    public RedstoneAddonScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public RedstoneAddonScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.REDSTONE_ADDON_SCREEN, syncId);
        
        this.blockPos = blockEntity.getBlockPos();
        this.blockEntity = (RedstoneAddonBlockEntity) blockEntity;
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
