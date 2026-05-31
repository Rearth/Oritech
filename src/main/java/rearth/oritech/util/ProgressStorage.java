package rearth.oritech.util;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class ProgressStorage implements ValueIOSerializable {

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressStorage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ProgressStorage::get,
            ProgressStorage::new
    );

    private int progress;
    private final ProgressJournal journal = new ProgressJournal();

    public ProgressStorage(int initialProgress) {
        this.progress = initialProgress;
    }

    public ProgressStorage() {
        this(0);
    }

    public void set(int value) {
        this.progress = value;
    }

    public void set(int value, TransactionContext transaction) {
        journal.updateSnapshots(transaction);
        this.progress = value;
    }

    public int get() {
        return progress;
    }

    public void increment(TransactionContext transaction) {
        journal.updateSnapshots(transaction);
        progress++;
    }

    public void decrement(TransactionContext transaction) {
        journal.updateSnapshots(transaction);
        progress--;
    }

    public void reset(TransactionContext transaction) {
        journal.updateSnapshots(transaction);
        progress = 0;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("progress", progress);
    }

    @Override
    public void deserialize(ValueInput input) {
        progress = input.getIntOr("progress", 0);
    }

    public boolean isEmpty() {
        return progress <= 0;
    }

    private class ProgressJournal extends SnapshotJournal<Integer> {

        @Override
        protected Integer createSnapshot() {
            return progress;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            progress = snapshot;
        }
    }
}
