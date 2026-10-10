package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.spaceage.init.SpaceAgeMenus;

/**
 * Configuration only: physical work cells replace machine inventories.
 */
public final class VacuumCrafterMenu extends AbstractContainerMenu {

    public final BlockPos pos;
    public final String name;

    public VacuumCrafterMenu(int id, Inventory player, RegistryFriendlyByteBuf buffer) {

        this(id, player, buffer.readBlockPos(), buffer.readUtf());
    }

    public VacuumCrafterMenu(int id, Inventory player, BlockPos pos, String name) {

        super(SpaceAgeMenus.VACUUM_CRAFTER.get(), id);
        this.pos = pos;
        this.name = name;
    }

    @Override
    public boolean stillValid(Player player) {

        return player.level().getBlockState(pos).getBlock() instanceof BlockPairController && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        return ItemStack.EMPTY;
    }
}
