package rearth.oritech.client.ui;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import rearth.oritech.block.entity.reactor.ReactorControllerBlockEntity;
import rearth.oritech.client.init.ModScreens;

import java.util.Objects;

public class ReactorScreenHandler extends ScreenHandler {
    
    public final ReactorControllerBlockEntity reactorEntity;
    public final World world;
    
    // this calls the second version
    public ReactorScreenHandler(int syncId, PlayerInventory inventory, PacketByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.getWorld().getBlockEntity(buf.readBlockPos())));
    }
    
    // on server, also called from client constructor
    public ReactorScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreens.REACTOR_SCREEN, syncId);
        
        reactorEntity = (ReactorControllerBlockEntity) blockEntity;
        world = blockEntity.getWorld();
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
