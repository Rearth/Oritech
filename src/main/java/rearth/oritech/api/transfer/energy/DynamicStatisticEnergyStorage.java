package rearth.oritech.api.transfer.energy;

import net.neoforged.neoforge.transfer.energy.DelegatingEnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.networking.SyncType;

import java.util.Arrays;

public class DynamicStatisticEnergyStorage extends DynamicEnergyStorage {

    private long insertedThisTick;
    private long extractedThisTick;
    private int insertOperationsThisTick;
    private final long[] historicInsert = new long[20];
    private final long[] historicExtract = new long[20];
    private int currentInsertSources = 0;
    private final StatisticsJournal statisticsJournal = new StatisticsJournal();
    private final EnergyHandler inputStorage;
    private final EnergyHandler outputStorage;

    public DynamicStatisticEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onUpdate) {
        super(capacity, maxInsert, maxExtract, 0, onUpdate, false);

        Arrays.fill(historicInsert, 0L);
        Arrays.fill(historicExtract, 0L);

        inputStorage = new DelegatingEnergyHandler(this) {
            @Override
            public int extract(int amount, TransactionContext transaction) {
                return 0;
            }
        };
        outputStorage = new DelegatingEnergyHandler(this) {
            @Override
            public int insert(int amount, TransactionContext transaction) {
                return 0;
            }
        };

    }

    public EnergyHandler getInputStorage() {
        return inputStorage;
    }

    public EnergyHandler getOutputStorage() {
        return outputStorage;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        var inserted = super.insert(amount, transaction);
        if (inserted > 0) {
            statisticsJournal.updateSnapshots(transaction);
            insertedThisTick += inserted;
            insertOperationsThisTick++;
        }
        return inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        var extracted = super.extract(amount, transaction);
        if (extracted > 0) {
            statisticsJournal.updateSnapshots(transaction);
            extractedThisTick += extracted;
        }
        return extracted;
    }

    public void tick(long worldTicks) {
        var index = (int) (worldTicks % 20);
        historicInsert[index] = insertedThisTick;
        historicExtract[index] = extractedThisTick;
        currentInsertSources = insertOperationsThisTick;

        insertedThisTick = 0;
        extractedThisTick = 0;
        insertOperationsThisTick = 0;
    }

    public EnergyStatistics getCurrentStatistics(long worldTicks) {
        var index = (int) (worldTicks % 20);

        return new EnergyStatistics(
                (float) Arrays.stream(historicInsert).average().orElse(0),
                (float) Arrays.stream(historicExtract).average().orElse(0),
                historicInsert[index],
                historicExtract[index],
                currentInsertSources,
                Arrays.stream(historicInsert).max().orElse(0),
                Arrays.stream(historicExtract).max().orElse(0)
        );

    }

    private class StatisticsJournal extends SnapshotJournal<StatisticSnapshot> {
        @Override
        protected StatisticSnapshot createSnapshot() {
            return new StatisticSnapshot(insertedThisTick, extractedThisTick, insertOperationsThisTick);
        }

        @Override
        protected void revertToSnapshot(StatisticSnapshot snapshot) {
            insertedThisTick = snapshot.insertedThisTick();
            extractedThisTick = snapshot.extractedThisTick();
            insertOperationsThisTick = snapshot.insertOperationsThisTick();
        }
    }

    private record StatisticSnapshot(long insertedThisTick, long extractedThisTick, int insertOperationsThisTick) {
    }

    public record EnergyStatistics(
            float avgInsertSecond,
            float avgExtractSecond,
            long insertedLastTickTotal,
            long extractedLastTickTotal,
            int insertionCountLastTick,
            long maxInsertSecond,
            long maxExtractSecond) {

        public static final EnergyStatistics EMPTY = new EnergyStatistics(0, 0, 0, 0, 0, 0, 0);

        @Override
        public @NotNull String toString() {
            return "EnergyStatistics{" +
                    "avgInsertSecond=" + avgInsertSecond +
                    ", avgExtractSecond=" + avgExtractSecond +
                    ", insertedLastTickTotal=" + insertedLastTickTotal +
                    ", extractedLastTickTotal=" + extractedLastTickTotal +
                    ", insertionCountLastTick=" + insertionCountLastTick +
                    ", maxInsertSecond=" + maxInsertSecond +
                    ", maxExtractSecond=" + maxExtractSecond +
                    '}';
        }
    }

    @Override
    public boolean useDeltaOnly(SyncType type) {
        return false;
    }
}
