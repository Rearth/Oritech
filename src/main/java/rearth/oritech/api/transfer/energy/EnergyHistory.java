package rearth.oritech.api.transfer.energy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rearth.oritech.api.networking.UpdatableField;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Thirty minutes of server-side history, recorded even when nobody has the GUI open.
 * Recent samples stay at one-second precision, then age into three- and nine-second averages.
 * History is deliberately not saved to disk. GUI_OPEN sends all tiers; GUI_TICK sends only
 * the latest one-second entry, which clients compact using the same rules as the server.
 */
public class EnergyHistory implements UpdatableField<EnergyHistory, EnergyHistory.Update> {

    public static final int SAMPLE_TICKS = 20;
    private static final int MAX_HISTORY_TICKS = 30 * 60 * SAMPLE_TICKS;
    // Includes up to two ungrouped entries at each tier boundary while waiting for a complete group of three.
    private static final int MAX_SAMPLES = 300;
    private final ArrayDeque<Sample> recent = new ArrayDeque<>();
    private final ArrayDeque<Sample> medium = new ArrayDeque<>();
    private final ArrayDeque<Sample> coarse = new ArrayDeque<>();
    private long latestTick = -1;
    // Stays fixed while old samples expire, so clients can distinguish a reset from normal updates.
    private long startedAt = -1;

    private static final StreamCodec<ByteBuf, Sample> SAMPLE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Sample::tick,
            ByteBufCodecs.VAR_LONG, Sample::stored,
            ByteBufCodecs.DOUBLE, Sample::inserted,
            ByteBufCodecs.DOUBLE, Sample::extracted,
            ByteBufCodecs.VAR_INT, Sample::spanTicks,
            Sample::new);
    private static final StreamCodec<ByteBuf, Update> UPDATE_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Update::startedAt,
            SAMPLE_CODEC.apply(ByteBufCodecs.list(MAX_SAMPLES)), Update::samples,
            Update::new);
    private static final StreamCodec<ByteBuf, EnergyHistory> FULL_CODEC = UPDATE_CODEC.map(update -> {
        var history = new EnergyHistory();
        history.handleDeltaUpdate(update);
        return history;
    }, history -> new Update(history.startedAt, history.getSamples()));

    /** Stored is the current RF amount; inserted/extracted are the existing last-second averages in RF/t. */
    public void record(long tick, long stored, double inserted, double extracted) {
        if (tick % SAMPLE_TICKS != 0) return;
        append(new Sample(tick, stored, inserted, extracted, SAMPLE_TICKS));
    }

    private void append(Sample sample) {
        // GUI updates arrive every tick, so the same one-second entry can be received repeatedly.
        if (sample.tick() <= latestTick) return;
        latestTick = sample.tick();
        if (startedAt < 0) startedAt = sample.tick();
        // Full snapshots already contain averaged samples; restore them directly into the matching tier.
        var tier = switch (sample.spanTicks()) {
            case SAMPLE_TICKS -> recent;
            case SAMPLE_TICKS * 3 -> medium;
            case SAMPLE_TICKS * 9 -> coarse;
            default -> throw new IllegalArgumentException("Unknown energy history resolution: " + sample.spanTicks());
        };
        tier.addLast(sample);
        compact(recent, medium, latestTick - 30 * SAMPLE_TICKS, SAMPLE_TICKS);
        compact(medium, coarse, latestTick - 5 * 60 * SAMPLE_TICKS, SAMPLE_TICKS * 3);
        // Expire by game time rather than entry count, so unloaded periods don't extend the window.
        expire(recent);
        expire(medium);
        expire(coarse);
    }

    private static void compact(ArrayDeque<Sample> source, ArrayDeque<Sample> target, long cutoff, int spanTicks) {
        while (source.size() >= 3) {
            var iterator = source.iterator();
            var first = iterator.next();
            var second = iterator.next();
            var third = iterator.next();
            // Leave at most two older entries in this tier until their whole group has aged out.
            if (third.tick() > cutoff) return;
            source.removeFirst();
            // Drop incomplete groups around unloaded gaps instead of averaging unrelated time periods together.
            if (second.tick() - first.tick() != spanTicks || third.tick() - second.tick() != spanTicks) continue;
            source.removeFirst();
            source.removeFirst();
            // Use the middle timestamp for the averaged interval. Division before addition avoids long overflow.
            target.addLast(new Sample(second.tick(),
                    Math.round(first.stored() / 3.0 + second.stored() / 3.0 + third.stored() / 3.0),
                    (first.inserted() + second.inserted() + third.inserted()) / 3,
                    (first.extracted() + second.extracted() + third.extracted()) / 3,
                    spanTicks * 3));
        }
    }

    private void expire(ArrayDeque<Sample> tier) {
        while (!tier.isEmpty() && tier.getFirst().tick() <= latestTick - MAX_HISTORY_TICKS)
            tier.removeFirst();
    }

    public List<Sample> getSamples() {
        // Tier promotion preserves chronological order. Return a stable snapshot for rendering and pause.
        var samples = new ArrayList<Sample>(coarse.size() + medium.size() + recent.size());
        samples.addAll(coarse);
        samples.addAll(medium);
        samples.addAll(recent);
        return List.copyOf(samples);
    }

    public void clear() {
        recent.clear();
        medium.clear();
        coarse.clear();
        latestTick = -1;
        startedAt = -1;
    }

    @Override
    public Update getDeltaData() {
        return new Update(startedAt, recent.isEmpty() ? List.of() : List.of(recent.getLast()));
    }

    @Override
    public EnergyHistory getFullData() {
        return this;
    }

    @Override
    public StreamCodec<ByteBuf, Update> getDeltaCodec() {
        return UPDATE_CODEC;
    }

    @Override
    public StreamCodec<ByteBuf, EnergyHistory> getFullCodec() {
        return FULL_CODEC;
    }

    @Override
    public void handleFullUpdate(EnergyHistory updatedData) {
        clear();
        handleDeltaUpdate(new Update(updatedData.startedAt, updatedData.getSamples()));
    }

    @Override
    public void handleDeltaUpdate(Update update) {
        // A network merge/split starts a new history; don't append its data to the old network's graph.
        if (startedAt != update.startedAt()) clear();
        startedAt = update.startedAt();
        update.samples().forEach(this::append);
    }

    // Tick is server game time, so recording follows simulation speed rather than wall-clock time.
    // spanTicks describes the represented interval, allowing graph joins and hover to follow each tier's resolution.
    public record Sample(long tick, long stored, double inserted, double extracted, int spanTicks) {
    }

    public record Update(long startedAt, List<Sample> samples) {
    }
}
