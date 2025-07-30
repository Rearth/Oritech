package rearth.oritech.block.entity.accelerator;

import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.NetworkedEventHandler;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.block.blocks.accelerator.AcceleratorPassthroughBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlackHoleBlockEntity extends NetworkedBlockEntity implements NetworkedEventHandler {
    
    public BlockState currentlyPulling;
    
    @SyncField
    public BlockPos currentlyPullingFrom;
    @SyncField
    public long pullingStartedAt;
    @SyncField
    public long pullTime;
    
    // if nothing is in influence, don't search so often
    private int waitTicks;
    
    // cache for outgoing hits
    private final Map<BlockPos, ParticleCollectorBlockEntity> cachedCollectors = new HashMap<>();
    
    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BLACK_HOLE_ENTITY, pos, state);
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (waitTicks-- > 0) return;
        
        if (currentlyPullingFrom != null && pullingStartedAt + pullTime - 5 < world.getGameTime()) {
            onPullingFinished();
            currentlyPullingFrom = null;
        }
        
        if (currentlyPullingFrom != null) return;
        
        int pullRange = Oritech.CONFIG.pullRange();
        
        for (var candidate : BlockPos.withinManhattan(pos, pullRange, pullRange, pullRange)) {
            var candidateState = world.getBlockState(candidate);
            if (candidate.equals(pos) || candidateState.isAir() || candidateState.is(TagContent.BLACK_HOLE_BLACKLIST) || candidateState.getFluidState().isSource() || candidateState.getBlock().equals(Blocks.MOVING_PISTON) || candidateState.getBlock().equals(BlockContent.BLACK_HOLE_BLOCK))
                continue;
            
            currentlyPullingFrom = candidate;
            currentlyPulling = candidateState;
            pullingStartedAt = world.getGameTime();
            pullTime = (long) candidate.distManhattan(pos) * Oritech.CONFIG.pullTimeMultiplier();
            world.setBlockAndUpdate(candidate, Blocks.AIR.defaultBlockState());
            setChanged();
            
            return;
        }
        
        if (currentlyPullingFrom == null) {
            waitTicks = Oritech.CONFIG.idleWaitTicks();
        }
    }
    
    private void onPullingFinished() {
        var from = currentlyPullingFrom;
        var pulledDir = Vec3.atLowerCornerOf(worldPosition.subtract(from));
        pulledDir = pulledDir.normalize();
        
        for (int i = 0; i < 5; i++) {
            var shootDir = pulledDir.offsetRandom(level.getRandom(), 0.5f);
            
            var cacheKey = getRayEnd(worldPosition.getCenter(), shootDir.normalize());
            var cachedHit = tryGetCachedCollector(cacheKey);
            if (cachedHit != null) {
                // re-use existing result
                ParticleContent.BLACK_HOLE_EMISSION.spawn(level, worldPosition.getCenter(), cachedHit.getBlockPos().getCenter());
                cachedHit.onParticleCollided();
            } else {
                // find target along exit line, and add it to cache
                var impactPos = basicRaycast(worldPosition.getCenter().add(pulledDir.scale(1.2)), shootDir, 12, level);
                if (impactPos != null) {
                    ParticleContent.BLACK_HOLE_EMISSION.spawn(level, worldPosition.getCenter(), impactPos.getCenter());
                    
                    var candidate = level.getBlockEntity(impactPos);
                    if (candidate instanceof ParticleCollectorBlockEntity collectorEntity) {
                        collectorEntity.onParticleCollided();
                        cachedCollectors.put(cacheKey, collectorEntity);
                    } else {
                        // only cast one particle if no collector has been found (for performance sake to avoid all those searches)
                        break;
                    }
                    
                } else {
                    // only cast one particle if no block has been found (for performance sake to avoid all those searches)
                    ParticleContent.BLACK_HOLE_EMISSION.spawn(level, worldPosition.getCenter(), worldPosition.getCenter().add(shootDir.scale(15)));
                    break;
                }
            }
        }
        
    }
    
    private static BlockPos getRayEnd(Vec3 shotFrom, Vec3 shotDirection) {
        return BlockPos.containing(shotFrom.add(shotDirection.scale(12)));
    }
    
    private ParticleCollectorBlockEntity tryGetCachedCollector(BlockPos key) {
        
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
    
    public static BlockPos basicRaycast(Vec3 from, Vec3 direction, int range, Level world) {
        
        var checkedPositions = new HashSet<BlockPos>();
        
        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.scale(i));
            var targetBlockPos = BlockPos.containing(to);
            
            // avoid double checks
            if (checkedPositions.contains(targetBlockPos)) continue;
            checkedPositions.add(targetBlockPos);
            
            var targetState = world.getBlockState(targetBlockPos);
            if (!canPassThrough(targetState, targetBlockPos)) return targetBlockPos;
        }
        
        return null;
    }
    
    
    private static boolean canPassThrough(BlockState state, BlockPos blockPos) {
        // When targetting entities, don't let grass, vines, small mushrooms, pressure plates, etc. get in the way of the laser
        return state.isAir() || state.getFluidState().isSource() || state.is(TagContent.LASER_PASSTHROUGH) || state.getBlock() instanceof AcceleratorPassthroughBlock;
    }
    
    @Override
    public void onNetworkUpdated() {
        if (currentlyPullingFrom != null)
            this.currentlyPulling = level.getBlockState(currentlyPullingFrom);
    }
}
