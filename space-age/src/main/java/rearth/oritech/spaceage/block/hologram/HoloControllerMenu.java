package rearth.oritech.spaceage.block.hologram;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.spaceage.init.SpaceAgeMenus;

public final class HoloControllerMenu extends AbstractContainerMenu {

    public final BlockPos pos;

    public HoloControllerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {

        this(id, inventory, buffer.readBlockPos());
    }

    public HoloControllerMenu(int id, Inventory inventory, BlockPos pos) {

        super(SpaceAgeMenus.HOLO_CONTROLLER.get(), id);
        this.pos = pos;
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockEntity(pos) instanceof HoloControllerBlockEntity
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        return ItemStack.EMPTY;
    }
}
