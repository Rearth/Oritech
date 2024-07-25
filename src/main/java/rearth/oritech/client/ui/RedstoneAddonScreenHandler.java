package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.machines.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.init.ModScreens;

public class RedstoneAddonScreenHandler extends ScreenHandler {
    
    @NotNull
    protected final BlockPos blockPos;
    @NotNull
    protected final RedstoneAddonBlockEntity blockEntity;
    
    public RedstoneAddonScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.REDSTONE_ADDON_SCREEN, syncId);
        
        this.blockPos = blockEntity.getPos();
        this.blockEntity = (RedstoneAddonBlockEntity) blockEntity;
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
