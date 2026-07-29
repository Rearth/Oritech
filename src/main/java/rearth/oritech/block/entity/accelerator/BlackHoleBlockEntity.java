package rearth.oritech.block.entity.accelerator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.blocks.accelerator.AcceleratorPassthroughBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BlackHoleBlockEntity extends NetworkedBlockEntity {

    private static final int BLOCKS_PER_GROWTH_STAGE = 100;
    private static final int PULL_FINISH_OFFSET = 5;

    @SyncField({SyncType.TICK, SyncType.INITIAL})
    public List<PulledBlock> currentlyPulling = new ArrayList<>();

    @SyncField({SyncType.TICK, SyncType.INITIAL})
    public int growth;

    // if nothing is in influence, don't search so often
    private int waitTicks;

    // cache for outgoing hits
    private final Map<BlockPos, TachyonAbsorberBlockEntity> cachedCollectors = new HashMap<>();

    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BLACK_HOLE.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        var changed = finishCompletedPulls(serverLevel.getGameTime());
        if (waitTicks-- > 0) {
            if (changed) setChanged();
            return;
        }

        if (currentlyPulling.size() >= getPullCapacity()) {
            if (changed) setChanged();
            return;
        }

        int pullRange = OritechConfig.pullRange.get();

        for (var candidate : BlockPos.withinManhattan(pos, pullRange, pullRange, pullRange)) {
            var candidateState = serverLevel.getBlockState(candidate);
            if (candidate.equals(pos) || candidateState.isAir() || candidateState.is(TagContent.BLACK_HOLE_BLACKLIST) || !candidateState.getFluidState().isEmpty() || candidateState.getBlock().equals(Blocks.MOVING_PISTON) || candidateState.getBlock().equals(BlockContent.BLACK_HOLE.get()))
                continue;

            var pullTime = (long) candidate.distManhattan(pos) * OritechConfig.pullTimeMultiplier.get();
            currentlyPulling.add(new PulledBlock(candidate.immutable(), Block.getId(candidateState), serverLevel.getGameTime(), pullTime));
            serverLevel.setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState());
            waitTicks = getPullInterval(pullTime) - 1;
            setChanged();

            return;
        }

        waitTicks = OritechConfig.idleWaitTicks.get();
        if (changed) setChanged();
    }

    private boolean finishCompletedPulls(long gameTime) {
        var changed = false;
        var iterator = currentlyPulling.iterator();

        while (iterator.hasNext()) {
            var pulledBlock = iterator.next();
            if (pulledBlock.startedAt() + pulledBlock.pullTime() - PULL_FINISH_OFFSET >= gameTime) continue;

            if (!onPullingFinished(pulledBlock.from()) && growth < Integer.MAX_VALUE) growth++;
            iterator.remove();
            changed = true;
        }

        return changed;
    }

    private boolean onPullingFinished(BlockPos from) {
        var pulledDir = Vec3.atLowerCornerOf(worldPosition.subtract(from));
        pulledDir = pulledDir.normalize();
        var tachyonCollected = false;

        for (int i = 0; i < 5; i++) {
            var shootDir = pulledDir.offsetRandom(level.getRandom(), 0.5f);

            var cacheKey = getRayEnd(worldPosition.getCenter(), shootDir.normalize());
            var cachedHit = tryGetCachedCollector(cacheKey);
            if (cachedHit != null) {
                // re-use existing result
                ParticleContent.BlackHoleEmission(level, worldPosition.getCenter(), cachedHit.getBlockPos().getCenter());
                cachedHit.onParticleCollided();
                tachyonCollected = true;
            } else {
                // find target along exit line, and add it to cache
                var impactPos = basicRaycast(worldPosition.getCenter().add(pulledDir.scale(1.2)), shootDir, 12, level);
                if (impactPos != null) {
                    ParticleContent.BlackHoleEmission(level, worldPosition.getCenter(), impactPos.getCenter());

                    var candidate = level.getBlockEntity(impactPos);
                    if (candidate instanceof TachyonAbsorberBlockEntity collectorEntity) {
                        collectorEntity.onParticleCollided();
                        cachedCollectors.put(cacheKey, collectorEntity);
                        tachyonCollected = true;
                    } else {
                        // only cast one particle if no collector has been found (for performance sake to avoid all those searches)
                        break;
                    }

                } else {
                    // only cast one particle if no block has been found (for performance sake to avoid all those searches)
                    ParticleContent.BlackHoleEmission(level, worldPosition.getCenter(), worldPosition.getCenter().add(shootDir.scale(15)));
                    break;
                }
            }
        }

        return tachyonCollected;
    }

    private int getPullCapacity() {
        return 1 + growth / BLOCKS_PER_GROWTH_STAGE;
    }

    private int getPullInterval(long pullTime) {
        var activePullTime = Math.max(1, pullTime - PULL_FINISH_OFFSET);
        var interval = Math.max(1, activePullTime / getPullCapacity());
        return (int) Math.min(Integer.MAX_VALUE, interval);
    }

    public float getGrowthScale() {
        return 1f + (float) Math.pow((double) growth / BLOCKS_PER_GROWTH_STAGE, 0.2);
    }

    private static BlockPos getRayEnd(Vec3 shotFrom, Vec3 shotDirection) {
        return BlockPos.containing(shotFrom.add(shotDirection.scale(12)));
    }

    private TachyonAbsorberBlockEntity tryGetCachedCollector(BlockPos key) {

        var cachedResult = cachedCollectors.get(key);
        if (cachedResult == null) {
            // no cache
            return null;
        } else if (cachedResult.isRemoved()) {
            cachedCollectors.remove(key);
            return null;
        }

        return cachedResult;
    }

    public static BlockPos basicRaycast(Vec3 from, Vec3 direction, int range, Level level) {

        var checkedPositions = new HashSet<BlockPos>();

        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.scale(i));
            var targetBlockPos = BlockPos.containing(to);

            // avoid double checks
            if (checkedPositions.contains(targetBlockPos)) continue;
            checkedPositions.add(targetBlockPos);

            var targetState = level.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
        }

        return null;
    }


    private static boolean canPassThrough(BlockState state, BlockPos blockPos) {
        // When targetting entities, don't let grass, vines, small mushrooms, pressure plates, etc. get in the way of the laser
        return state.isAir() || !state.getFluidState().isEmpty() || state.is(TagContent.LASER_PASSTHROUGH) || state.getBlock() instanceof AcceleratorPassthroughBlock;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("growth", growth);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        growth = Math.max(0, input.getIntOr("growth", 0));
    }

    public record PulledBlock(BlockPos from, int stateId, long startedAt, long pullTime) {

        public BlockState state() {
            return Block.stateById(stateId);
        }
    }
}
