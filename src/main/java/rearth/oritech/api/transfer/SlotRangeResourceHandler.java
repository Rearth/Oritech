package rearth.oritech.api.transfer;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * An index-remapped view over a fixed, contiguous slot range {@code [start, start + count)} of a backing
 * {@link ResourceHandler}, with NO additional restrictions on insertion/extraction.
 * <p>
 * This exists because machines usually expose one {@link ResourceHandler} externally (e.g. to pipes)
 * that restricts insertion to input slots and extraction to output slots. That's correct for the
 * outside world, but internal logic (recipe crafting, {@code canOutputRecipe} checks, GUIs, amount
 * checks, etc.) often needs full, unrestricted insert/extract access to just the input slots or just
 * the output slots. This class provides exactly that: a small, reusable "sub view" that translates
 * local indices to real backing-storage indices and forwards to caller-supplied raw (unrestricted)
 * insert/extract functions, bypassing whatever external-facing restrictions the backing handler applies.
 * <p>
 * The slot range is assumed to be fixed for the lifetime of the handler (input/output slot counts never change).
 * <p>
 * Used by {@code InOutInventoryStorage} and {@code InOutFluidStorage} to build their
 * {@code getInputContainer()} / {@code getOutputContainer()} views.
 */
public class SlotRangeResourceHandler<T extends Resource> implements ResourceHandler<T> {

    @FunctionalInterface
    public interface IndexedTransfer<T extends Resource> {
        int apply(int index, T resource, int amount, TransactionContext transaction);
    }

    private final ResourceHandler<T> backing;  // used for read-only lookups, which are unrestricted anyway
    private final int start;
    private final int count;
    private final IndexedTransfer<T> rawInsert;
    private final IndexedTransfer<T> rawExtract;

    public SlotRangeResourceHandler(ResourceHandler<T> backing, int start, int count, IndexedTransfer<T> rawInsert, IndexedTransfer<T> rawExtract) {
        this.backing = backing;
        this.start = start;
        this.count = count;
        this.rawInsert = rawInsert;
        this.rawExtract = rawExtract;
    }

    private int real(int index) {
        return index + start;
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public T getResource(int index) {
        return backing.getResource(real(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return backing.getAmountAsLong(real(index));
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return backing.getCapacityAsLong(real(index), resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return backing.isValid(real(index), resource);
    }

    @Override
    public int insert(int index, T resource, int amount, TransactionContext transaction) {
        if (index < 0 || index >= size()) return 0;
        return rawInsert.apply(real(index), resource, amount, transaction);
    }

    // uses custom loop to allow using the rawInsert variant. Same for extract.
    @Override
    public int insert(T resource, int amount, TransactionContext transaction) {
        var remaining = amount;
        for (int i = 0; i < size() && remaining > 0; i++) {
            remaining -= insert(i, resource, remaining, transaction);
        }
        return amount - remaining;
    }

    @Override
    public int extract(int index, T resource, int amount, TransactionContext transaction) {
        if (index < 0 || index >= size()) return 0;
        return rawExtract.apply(real(index), resource, amount, transaction);
    }

    @Override
    public int extract(T resource, int amount, TransactionContext transaction) {
        var remaining = amount;
        for (int i = 0; i < size() && remaining > 0; i++) {
            remaining -= extract(i, resource, remaining, transaction);
        }
        return amount - remaining;
    }
}

