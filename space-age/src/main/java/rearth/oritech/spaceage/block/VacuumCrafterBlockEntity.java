package rearth.oritech.spaceage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rearth.oritech.spaceage.init.SpaceAgeBlockEntities;

/**
 * Placed controllers only retain their optional name; rocket batteries supply processing power.
 */
public final class VacuumCrafterBlockEntity extends BlockEntity implements MenuProvider {

    private String name = "";

    public VacuumCrafterBlockEntity(BlockPos pos, BlockState state) {

        super(SpaceAgeBlockEntities.VACUUM_CRAFTER.get(), pos, state);
    }

    public String name() {

        return name;
    }

    public void setName(String value) {

        var trimmed = value.strip();
        name = trimmed.substring(0, Math.min(trimmed.length(), 32));
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {

        super.saveAdditional(output);
        output.putString("name", name);
    }

    @Override
    protected void loadAdditional(ValueInput input) {

        super.loadAdditional(input);
        name = input.getStringOr("name", "");
    }

    @Override
    public Component getDisplayName() {

        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {

        return new VacuumCrafterMenu(id, inventory, getBlockPos(), name);
    }
}
