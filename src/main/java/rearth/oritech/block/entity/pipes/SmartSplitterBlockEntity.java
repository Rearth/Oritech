package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;
import rearth.oritech.block.blocks.pipes.item.SmartSplitterBlock;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class SmartSplitterBlockEntity extends BlockEntity implements ItemProvider {

    public static final int OVERFLOW_DELAY = 20;
    private static final Direction[] OUTPUT_DIRECTIONS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public final SimpleInventoryStorage inventory = new SimpleInventoryStorage(1, this::setChanged);

    private final int[] reservations = new int[OUTPUT_DIRECTIONS.length];
    private final long[] lastProgress = new long[OUTPUT_DIRECTIONS.length];
    private final EnumMap<Direction, ResourceHandler<ItemResource>> sidedHandlers = new EnumMap<>(Direction.class);
    private final ResourceHandler<ItemResource> unsidedHandler = new SplitterItemHandler(null);
    private final SplitterStateJournal stateJournal = new SplitterStateJournal();

    private SplitMode mode = SplitMode.STRICT;
    private int remainderCursor;
    private int roundRobinCursor;
    private long turnStartedAt;

    public SmartSplitterBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.SMART_SPLITTER.get(), pos, state);
        for (var direction : Direction.values()) {
            sidedHandlers.put(direction, new SplitterItemHandler(direction));
        }
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return direction == null ? unsidedHandler : sidedHandlers.get(direction);
    }

    public SplitMode getMode() {
        return mode;
    }

    public SplitMode cycleMode() {
        mode = mode.next();
        if (mode != SplitMode.ROUND_ROBIN) {
            rebalanceStoredItems();
        } else {
            normalizeRoundRobinCursor();
            turnStartedAt = gameTime();
            setChanged();
        }
        return mode;
    }

    public void onOutputConfigurationChanged() {
        if (mode == SplitMode.ROUND_ROBIN) {
            normalizeRoundRobinCursor();
            turnStartedAt = gameTime();
            setChanged();
        } else {
            rebalanceStoredItems();
        }
    }

    public void serverTick(Level level) {
        if (level.isClientSide() || inventory.isEmpty() || level.getGameTime() % 5 != 0) return;

        if (mode == SplitMode.OVERFLOW) {
            redistributeStaleReservations(level.getGameTime());
        } else if (mode == SplitMode.ROUND_ROBIN
                && activeOutputs().size() > 1
                && level.getGameTime() - turnStartedAt >= OVERFLOW_DELAY) {
            advanceRoundRobin(currentRoundRobinOutput());
            turnStartedAt = level.getGameTime();
            setChanged();
        }
    }

    private int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        var wasEmpty = inventory.isEmpty();
        var inserted = inventory.insert(index, resource, amount, transaction);
        if (inserted <= 0) return 0;

        if (mode == SplitMode.ROUND_ROBIN) {
            if (wasEmpty) {
                stateJournal.updateSnapshots(transaction);
                normalizeRoundRobinCursor();
                turnStartedAt = gameTime();
            }
        } else {
            addReservations(inserted, activeOutputs(), transaction);
        }

        return inserted;
    }

    private int extract(@Nullable Direction side, int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (side == null || !isConfiguredOutput(side) || amount == 0) return 0;

        var sideIndex = outputIndex(side);
        var allowed = amount;

        if (mode == SplitMode.ROUND_ROBIN) {
            if (currentRoundRobinOutput() != side) return 0;
        } else {
            allowed = Math.min(allowed, reservations[sideIndex]);
            if (allowed <= 0) return 0;
        }

        var extracted = inventory.extract(index, resource, allowed, transaction);
        if (extracted <= 0) return 0;

        stateJournal.updateSnapshots(transaction);
        lastProgress[sideIndex] = gameTime();

        if (mode == SplitMode.ROUND_ROBIN) {
            advanceRoundRobin(side);
            turnStartedAt = gameTime();
        } else {
            reservations[sideIndex] -= extracted;
        }

        return extracted;
    }

    private void addReservations(int amount, List<Direction> outputs, TransactionContext transaction) {
        if (amount <= 0 || outputs.isEmpty()) return;

        stateJournal.updateSnapshots(transaction);
        distribute(amount, outputs, gameTime());
    }

    private void distribute(int amount, List<Direction> outputs, long now) {
        var evenShare = amount / outputs.size();
        if (evenShare > 0) {
            for (var output : outputs) addReservation(output, evenShare, now);
        }

        var remainder = amount % outputs.size();
        while (remainder-- > 0) {
            var output = nextOutput(outputs, remainderCursor);
            addReservation(output, 1, now);
            remainderCursor = (outputIndex(output) + 1) % OUTPUT_DIRECTIONS.length;
        }
    }

    private void addReservation(Direction output, int amount, long now) {
        var index = outputIndex(output);
        if (reservations[index] == 0) lastProgress[index] = now;
        reservations[index] += amount;
    }

    private void rebalanceStoredItems() {
        java.util.Arrays.fill(reservations, 0);
        var outputs = activeOutputs();
        if (!outputs.isEmpty()) distribute(inventory.getAmountAsInt(0), outputs, gameTime());
        setChanged();
    }

    private void redistributeStaleReservations(long now) {
        var active = activeOutputs();
        if (active.size() < 2) return;

        var stale = new ArrayList<Direction>();
        var recipients = new ArrayList<Direction>();
        for (var output : active) {
            var index = outputIndex(output);
            if (reservations[index] > 0 && now - lastProgress[index] >= OVERFLOW_DELAY) {
                stale.add(output);
            } else {
                recipients.add(output);
            }
        }

        if (stale.isEmpty() || recipients.isEmpty()) return;

        var reclaimed = 0;
        for (var output : stale) {
            var index = outputIndex(output);
            reclaimed += reservations[index];
            reservations[index] = 0;
        }
        distribute(reclaimed, recipients, now);
        setChanged();
    }

    private void normalizeRoundRobinCursor() {
        var current = currentRoundRobinOutput();
        if (current != null) roundRobinCursor = outputIndex(current);
    }

    private void advanceRoundRobin(@Nullable Direction completedOutput) {
        var start = completedOutput == null ? roundRobinCursor + 1 : outputIndex(completedOutput) + 1;
        var outputs = activeOutputs();
        if (outputs.isEmpty()) return;
        roundRobinCursor = outputIndex(nextOutput(outputs, start));
    }

    private @Nullable Direction currentRoundRobinOutput() {
        var outputs = activeOutputs();
        return outputs.isEmpty() ? null : nextOutput(outputs, roundRobinCursor);
    }

    private static Direction nextOutput(List<Direction> outputs, int start) {
        for (int offset = 0; offset < OUTPUT_DIRECTIONS.length; offset++) {
            var candidate = OUTPUT_DIRECTIONS[Math.floorMod(start + offset, OUTPUT_DIRECTIONS.length)];
            if (outputs.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("No splitter output available");
    }

    private List<Direction> activeOutputs() {
        var result = new ArrayList<Direction>(OUTPUT_DIRECTIONS.length);
        for (var direction : OUTPUT_DIRECTIONS) {
            if (isConfiguredOutput(direction)) result.add(direction);
        }
        return result;
    }

    private boolean isConfiguredOutput(Direction direction) {
        return direction.getAxis().isHorizontal() && SmartSplitterBlock.isOutput(getBlockState(), direction);
    }

    private long gameTime() {
        return level == null ? 0 : level.getGameTime();
    }

    private static int outputIndex(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("Not a horizontal direction: " + direction);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output);
        output.putInt("split_mode", mode.ordinal());
        output.putInt("remainder_cursor", remainderCursor);
        output.putInt("round_robin_cursor", roundRobinCursor);
        output.putLong("turn_started_at", turnStartedAt);

        for (var direction : OUTPUT_DIRECTIONS) {
            var name = direction.getName();
            var index = outputIndex(direction);
            output.putInt("reserved_" + name, reservations[index]);
            output.putLong("last_progress_" + name, lastProgress[index]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input);
        mode = SplitMode.fromOrdinal(input.getIntOr("split_mode", 0));
        remainderCursor = Math.floorMod(input.getIntOr("remainder_cursor", 0), OUTPUT_DIRECTIONS.length);
        roundRobinCursor = Math.floorMod(input.getIntOr("round_robin_cursor", 0), OUTPUT_DIRECTIONS.length);
        turnStartedAt = input.getLongOr("turn_started_at", 0);

        for (var direction : OUTPUT_DIRECTIONS) {
            var name = direction.getName();
            var index = outputIndex(direction);
            reservations[index] = Math.max(0, input.getIntOr("reserved_" + name, 0));
            lastProgress[index] = input.getLongOr("last_progress_" + name, 0);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) level.blockEntityChanged(worldPosition);
    }

    private class SplitterItemHandler implements ResourceHandler<ItemResource> {
        private final @Nullable Direction side;

        private SplitterItemHandler(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public int size() {
            return inventory.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return inventory.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return inventory.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return inventory.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return inventory.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return SmartSplitterBlockEntity.this.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return SmartSplitterBlockEntity.this.extract(side, index, resource, amount, transaction);
        }
    }

    private class SplitterStateJournal extends SnapshotJournal<SplitterSnapshot> {
        @Override
        protected SplitterSnapshot createSnapshot() {
            return new SplitterSnapshot(
                    reservations.clone(),
                    lastProgress.clone(),
                    remainderCursor,
                    roundRobinCursor,
                    turnStartedAt
            );
        }

        @Override
        protected void revertToSnapshot(SplitterSnapshot snapshot) {
            System.arraycopy(snapshot.reservations(), 0, reservations, 0, reservations.length);
            System.arraycopy(snapshot.lastProgress(), 0, lastProgress, 0, lastProgress.length);
            remainderCursor = snapshot.remainderCursor();
            roundRobinCursor = snapshot.roundRobinCursor();
            turnStartedAt = snapshot.turnStartedAt();
        }

        @Override
        protected void onRootCommit(SplitterSnapshot originalState) {
            setChanged();
        }
    }

    private record SplitterSnapshot(int[] reservations, long[] lastProgress, int remainderCursor,
                                    int roundRobinCursor, long turnStartedAt) {
    }

    public enum SplitMode {
        STRICT,
        OVERFLOW,
        ROUND_ROBIN;

        public SplitMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        private static SplitMode fromOrdinal(int ordinal) {
            return values()[Math.clamp(ordinal, 0, values().length - 1)];
        }

        public String translationKey() {
            return "message.oritech.smart_splitter.mode." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
