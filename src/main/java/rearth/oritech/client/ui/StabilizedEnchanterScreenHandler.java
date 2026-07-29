package rearth.oritech.client.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.block.entity.arcane.StabilizedEnchanterBlockEntity;

import java.util.Objects;

public class StabilizedEnchanterScreenHandler extends OritechScreenHandler {

    protected final StabilizedEnchanterBlockEntity stabilized_enchanter;

    public StabilizedEnchanterScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }

    public StabilizedEnchanterScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);

        stabilized_enchanter = (StabilizedEnchanterBlockEntity) this.blockEntity;
    }
}
