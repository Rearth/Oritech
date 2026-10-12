package rearth.oritech.block.entity.pipes;

/**
 * Server-only, transient scheduling state for an individual pipe interface.
 */
public class PipeTransferBackoff {

    private final int idleThreshold;
    private final int maxInterval;
    private final int tickOffset;
    private long idleSinceTick = -1;
    private long nextAttemptTick;
    private int retryInterval;

    PipeTransferBackoff(int idleThreshold, int maxInterval, int tickOffset) {
        this.idleThreshold = idleThreshold;
        this.maxInterval = maxInterval;
        this.tickOffset = tickOffset;
    }

    boolean shouldAttempt(long gameTime, int normalInterval) {
        if (retryInterval > 0) return gameTime >= nextAttemptTick;
        return Math.floorMod(gameTime, normalInterval) == Math.floorMod(tickOffset, normalInterval);
    }

    void recordAttempt(long gameTime, int normalInterval, boolean transferred) {
        if (transferred) {
            reset();
            idleSinceTick = gameTime;
            return;
        }

        if (idleSinceTick < 0) idleSinceTick = gameTime;
        if (gameTime - idleSinceTick < idleThreshold) return;

        // Never poll faster than the configured normal interval, and multiply as a long to avoid overflow.
        retryInterval = Math.clamp(2L * Math.max(normalInterval, retryInterval), normalInterval, Math.max(normalInterval, maxInterval));
        nextAttemptTick = gameTime + retryInterval;
    }

    void reset() {
        idleSinceTick = -1;
        nextAttemptTick = 0;
        retryInterval = 0;
    }
}
