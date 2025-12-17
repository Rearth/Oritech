package rearth.oritech.api.energy.containers;

import org.jetbrains.annotations.NotNull;
import rearth.oritech.api.networking.SyncType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicStatisticEnergyStorage extends DynamicEnergyStorage {
    
    private final List<Long> inserted = new ArrayList<>();  // just for this tick
    private final List<Long> extracted = new ArrayList<>();
    private final Long[] historicInsert = new Long[20];
    private final Long[] historicExtract = new Long[20];
    private int currentInsertSources = 0;
    
    public DynamicStatisticEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onUpdate) {
        super(capacity, maxInsert, maxExtract, onUpdate);
        
        Arrays.fill(historicInsert, 0L);
        Arrays.fill(historicExtract, 0L);
    }
    
    @Override
    public long insert(long amount, boolean simulate) {
        
        var inserted = super.insert(amount, simulate);
        if (!simulate && inserted > 0) {
            this.inserted.add(inserted);
        }
        
        return inserted;
    }
    
    @Override
    public long extract(long amount, boolean simulate) {
        
        var extracted = super.extract(amount, simulate);
        if (!simulate && extracted > 0) {
            this.extracted.add(extracted);
        }
        
        return extracted;
    }
    
    public void tick(long worldTicks) {
        var index = (int) (worldTicks % 20);
        historicInsert[index] = inserted.stream().mapToLong(Long::longValue).sum();
        historicExtract[index] = extracted.stream().mapToLong(Long::longValue).sum();
        currentInsertSources = inserted.size();
        
        inserted.clear();
        extracted.clear();
    }
    
    public EnergyStatistics getCurrentStatistics(long worldTicks) {
        var index = (int) (worldTicks % 20);
        
        return new EnergyStatistics(
          (float) Arrays.stream(historicInsert).mapToLong(Long::longValue).average().orElse(0),
          (float) Arrays.stream(historicExtract).mapToLong(Long::longValue).average().orElse(0),
          historicInsert[index],
          historicExtract[index],
          currentInsertSources,
          Arrays.stream(historicInsert).mapToLong(Long::longValue).max().orElse(0),
          Arrays.stream(historicExtract).mapToLong(Long::longValue).max().orElse(0)
        );
        
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
