package rearth.oritech.api.transfer.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

import java.util.List;

public class SimpleInventoryStorage extends ItemStacksResourceHandler implements UpdatableField<Void, List<ItemStack>> {

    private final Runnable onUpdate;

    public SimpleInventoryStorage(int size, Runnable onUpdate) {
        super(size);
        this.onUpdate = onUpdate;
    }

    public SimpleInventoryStorage(NonNullList<ItemStack> stacks, Runnable onUpdate) {
        super(stacks);
        this.onUpdate = onUpdate;
    }

    @Override
    public List<ItemStack> getDeltaData() {
        return stacks;
    }

    @Override
    public Void getFullData() {
        return null;
    }

    @Override
    public StreamCodec<? extends ByteBuf, List<ItemStack>> getDeltaCodec() {
        return ItemStack.OPTIONAL_LIST_STREAM_CODEC;
    }

    @Override
    public StreamCodec<? extends ByteBuf, Void> getFullCodec() {
        return null;
    }

    @Override
    public boolean useDeltaOnly(SyncType type) {
        return true;
    }

    @Override
    public void handleFullUpdate(Void updatedData) {
    }

    @Override
    public void handleDeltaUpdate(List<ItemStack> updatedData) {

        this.stacks.clear();

        for (int i = 0; i < updatedData.size(); i++) {
            var added = updatedData.get(i);
            this.stacks.set(i, added);
        }

    }

    @Override
    protected void onContentsChanged(int index, ItemStack previousContents) {
        super.onContentsChanged(index, previousContents);
        onUpdate.run();
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    public ItemStack getItem(int index) {
        return getStacks().get(index);
    }

    public void serialize(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), stacks);
    }

    public void deserialize(ValueInput input) {
        input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(this::handleDeltaUpdate);
    }
}
