package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.machines.accelerator.AcceleratorPassthroughBlock;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.network.NetworkContent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class BlackHoleBlockEntity extends BlockEntity implements BlockEntityTicker<BlackHoleBlockEntity> {
    public BlockPos currentlyPullingFrom;
    public BlockState currentlyPulling;
    public long pullingStartedAt;
    public long pullTime;
    
    // if nothing is in influence, don't search so often
    private int waitTicks;
    
    // cache for outgoing hits
    private final Map<BlockPos, ParticleCollectorBlockEntity> cachedCollectors = new HashMap<>();
    
    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BLACK_HOLE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, BlackHoleBlockEntity blockEntity) {
        if (world.isClient || waitTicks-- > 0) return;
        
        if (currentlyPullingFrom != null && pullingStartedAt + pullTime - 5 < world.getTime()) {
            onPullingFinished();
            currentlyPullingFrom = null;
        }
        
        if (currentlyPullingFrom != null) return;

        int pullRange = Oritech.CONFIG.pullRange();

        for (var candidate : BlockPos.iterateOutwards(pos, pullRange, pullRange, pullRange)) {
            var candidateState = world.getBlockState(candidate);
            if (candidate.equals(pos) || candidateState.isAir() || candidateState.isLiquid() || candidateState.getBlock().equals(Blocks.MOVING_PISTON) || candidateState.getBlock().equals(BlockContent.BLACK_HOLE_BLOCK)) continue;
            
            currentlyPullingFrom = candidate;
            currentlyPulling = candidateState;
            pullingStartedAt = world.getTime();
            pullTime = (long) candidate.getManhattanDistance(pos) * Oritech.CONFIG.pullTimeMultiplier();
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.BlackHoleSuckPacket(pos, currentlyPullingFrom, pullingStartedAt, pullTime));
            world.setBlockState(candidate, Blocks.AIR.getDefaultState());
            return;
        }
        
        if (currentlyPullingFrom == null) {
            waitTicks = Oritech.CONFIG.idleWaitTicks();
        }
    }
    
    private void onPullingFinished() {
        var from = currentlyPullingFrom;
        var pulledDir = Vec3d.of(pos.subtract(from));
        pulledDir = pulledDir.normalize();
        
        for (int i = 0; i < 5; i++) {
            var shootDir = pulledDir.addRandom(world.getRandom(), 0.5f);
            
            var cacheKey = getRayEnd(pos.toCenterPos(), shootDir.normalize());
            var cachedHit = tryGetCachedCollector(cacheKey);
            if (cachedHit != null) {
                // re-use existing result
                ParticleContent.BLACK_HOLE_EMISSION.spawn(world, pos.toCenterPos(), cachedHit.getPos().toCenterPos());
                cachedHit.onParticleCollided();
            } else {
                // find target along exit line, and add it to cache
                var impactPos = basicRaycast(pos.toCenterPos().add(pulledDir.multiply(1.2)), shootDir, 12, world);
                if (impactPos != null) {
                    ParticleContent.BLACK_HOLE_EMISSION.spawn(world, pos.toCenterPos(), impactPos.toCenterPos());
                    
                    var candidate = world.getBlockEntity(impactPos);
                    if (candidate instanceof ParticleCollectorBlockEntity collectorEntity) {
                        collectorEntity.onParticleCollided();
                        cachedCollectors.put(cacheKey, collectorEntity);
                    } else {
                        // only cast one particle if no collector has been found (for performance sake to avoid all those searches)
                        break;
                    }
                    
                } else {
                    // only cast one particle if no block has been found (for performance sake to avoid all those searches)
                    ParticleContent.BLACK_HOLE_EMISSION.spawn(world, pos.toCenterPos(), pos.toCenterPos().add(shootDir.multiply(15)));
                    break;
                }
            }
        }
        
    }
    
    private static BlockPos getRayEnd(Vec3d shotFrom, Vec3d shotDirection) {
        return BlockPos.ofFloored(shotFrom.add(shotDirection.multiply(12)));
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
    
    public static BlockPos basicRaycast(Vec3d from, Vec3d direction, int range, World world) {
        
        var checkedPositions = new HashSet<BlockPos>();
        
        for (float i = 0; i < range; i += 0.3f) {
            var to = from.add(direction.multiply(i));
            var targetBlockPos = BlockPos.ofFloored(to);
            
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
        return state.isAir() || state.isLiquid() || state.isIn(TagContent.LASER_PASSTHROUGH) || state.getBlock() instanceof AcceleratorPassthroughBlock;
    }
    
    public void onClientPullEvent(NetworkContent.BlackHoleSuckPacket packet) {
        this.currentlyPullingFrom = packet.from();
        this.pullTime = packet.duration();
        this.pullingStartedAt = world.getTime();
        this.currentlyPulling = world.getBlockState(packet.from());
    }
    
}
