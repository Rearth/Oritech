package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.machines.accelerator.AcceleratorRingBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.Geometry;

import java.util.*;

public class AcceleratorControllerBlockEntity extends BlockEntity implements BlockEntityTicker<AcceleratorControllerBlockEntity> {
    
    private static final int MAX_VELOCITY = 5000;
    private static final Map<CompPair<BlockPos, Vec3i>, BlockPos> cachedGates = new HashMap<>();    // stores the next gate for a combo of source gate and direction
    
    private ActiveParticle particle;
    
    // client data
    public List<Vec3d> displayTrail;
    
    public AcceleratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ACCELERATOR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }
    
    @SuppressWarnings("lossy-conversions")
    @Override
    public void tick(World world, BlockPos pos, BlockState state, AcceleratorControllerBlockEntity blockEntity) {
        if (world.isClient) return;
        
        var timePassed = 1 / 20f;
        
        if (particle == null) return;
        
        var renderedTrail = new ArrayList<Vec3d>();
        renderedTrail.add(particle.position);
        
        var availableDistance = particle.velocity * timePassed;
        while (availableDistance > 0.001) {
            
            if (particle.nextGate == null) {
                particle = null;
                return;
            }
            
            var path = particle.nextGate.toCenterPos().subtract(particle.position);
            var pathLength = path.length();
            var moveDist = Math.min(pathLength, availableDistance);
            availableDistance -= moveDist;
            var movedBy = path.normalize().multiply(moveDist);
            particle.position = particle.position.add(movedBy);
            renderedTrail.add(particle.position);
            particle.lastBendDistance += moveDist;
            
            if (moveDist >= pathLength - 0.1f) {
                // gate reached
                // calculate next gate direction
                var reachedGate = particle.nextGate;
                var nextDirection = getGateExitDirection(particle.lastGate, particle.nextGate);
                // try find next valid gate
                var nextGate = findNextGateCached(reachedGate, nextDirection, particle.velocity);
                
                // check if curve is too strong (based on reached gate)
                var gateOffset = particle.nextGate.subtract(particle.lastGate);
                var lastDirection = new Vec3i(Math.clamp(gateOffset.getX(), -1, 1), 0, Math.clamp(gateOffset.getZ(), -1, 1));
                var wasBend = !lastDirection.equals(nextDirection);
                if (wasBend) {
                    
                    var combinedDist = particle.lastBendDistance + particle.lastBendDistance2;
                    
                    var requiredDist = Math.sqrt(particle.velocity) / 5;
                    if (combinedDist <= requiredDist) {
                        System.out.println("too fast! speed: " + particle.velocity + " at bend dist " + combinedDist);
                        particle = null;
                        return;
                    }
                    
                    particle.lastBendDistance2 = particle.lastBendDistance;
                    particle.lastBendDistance = 0;
                }
                
                var reachedMotor = world.getBlockState(reachedGate).getBlock().equals(BlockContent.ACCELERATOR_MOTOR);
                if (reachedMotor) {
                    particle.velocity += 1;
                }
                
                particle.nextGate = nextGate;
                particle.lastGate = reachedGate;
            }
        }
        
        if (renderedTrail.size() > 1) {
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.AcceleratorParticleRenderPacket(pos, renderedTrail));
        }
    }
    
    public void test() {
        
        var facing = getCachedState().get(Properties.HORIZONTAL_FACING);
        var posBehind = Geometry.offsetToWorldPosition(facing, new Vec3i(1, 0, 0), pos);
        var directionRight = Geometry.getRight(facing);
        
        var candidateBlock = world.getBlockState(new BlockPos(posBehind));
        if (candidateBlock.getBlock().equals(BlockContent.ACCELERATOR_RING)) {
            
            var startPosition = (BlockPos) posBehind;
            var nextGate = findNextGate(startPosition, directionRight, 1);
            System.out.println("gate: " + nextGate);
            particle = new ActiveParticle(startPosition.toCenterPos(), 1, nextGate, startPosition);
        }
    }
    
    // this assumes the next gate is a valid target for a particle coming from lastGate.
    // Returns a neighboring or diagonal direction
    private Vec3i getGateExitDirection(BlockPos lastGate, BlockPos nextGate) {
        
        var incomingPath = nextGate.subtract(lastGate);
        var incomingStraight = incomingPath.getX() == 0 || incomingPath.getZ() == 0;
        var incomingDir = new Vec3i(Math.clamp(incomingPath.getX(), -1, 1), 0, Math.clamp(incomingPath.getZ(), -1, 1));
        
        var targetState = world.getBlockState(nextGate);
        
        // go straight through motors
        if (targetState.getBlock().equals(BlockContent.ACCELERATOR_MOTOR)) return incomingDir;
        
        // if the target gate has just been destroyed
        if (!targetState.getBlock().equals(BlockContent.ACCELERATOR_RING)) return incomingDir;
        
        var targetFacing = targetState.get(Properties.HORIZONTAL_FACING);
        var targetBent = targetState.get(AcceleratorRingBlock.BENT);
        var targetRedstone = targetState.get(AcceleratorRingBlock.REDSTONE_STATE);
        
        // if we come straight, the exit can be either curved or bent
        // if we come bent, the exit has to be straight
        
        // if we come in straight, redstone is 0, and we come in from the front (straight), we exit straight (weird edge case) (e.g. we arrive at the entrance enabled with redstone)
        if (targetRedstone == 0 && incomingStraight && Geometry.getBackward(targetFacing).equals(incomingDir)) {
            return Geometry.getBackward(targetFacing);
        }
        
        if (!incomingStraight) {
            // if we come in from the bent side, we always exit at the back of nextGate
            return Geometry.getBackward(targetFacing);
        } else {
            // if we come in straight, we either exit straight or bent
            if (targetBent == 0) {  // straight, keep direction. We don't know whether we enter from forward or behind
                return incomingDir;
            } else if (targetBent == 1) {   // bent left
                return Geometry.getForward(targetFacing).add(Geometry.getLeft(targetFacing));
            } else {   // bent right
                return Geometry.getForward(targetFacing).add(Geometry.getRight(targetFacing));
            }
        }
        
    }
    
    @Nullable
    private BlockPos findNextGateCached(BlockPos from, Vec3i direction, float speed) {
        
        var maxDist = Math.clamp(Math.sqrt(speed), 2, 8);
        var key = new CompPair<>(from, direction);
        
        if (cachedGates.containsKey(key)) {
            var result = cachedGates.get(key);
            var dist = (int) result.toCenterPos().distanceTo(from.toCenterPos());
            if (dist <= maxDist) return result;
            return null;
        }
        
        System.out.println("cache miss");
        var candidate = findNextGate(from, direction, speed);
        if (candidate != null) {
            cachedGates.put(key, candidate);
        }
        
        return candidate;
        
    }
    
    // tries to find the next gate candidate, based on the starting gate
    // direction can be either straight or diagonal
    @Nullable
    private BlockPos findNextGate(BlockPos from, Vec3i direction, float speed) {
        
        // longer empty areas only work at higher speeds
        var maxDist = Math.clamp(Math.sqrt(speed), 2, 8);
        
        for (int i = 1; i <= maxDist; i++) {
            var candidatePos = from.add(direction.multiply(i));
            var candidateState = world.getBlockState(candidatePos);
            if (candidateState.isAir()) continue;
            
            if (candidateState.getBlock().equals(BlockContent.ACCELERATOR_MOTOR)) return candidatePos;
            
            if (!candidateState.getBlock().equals(BlockContent.ACCELERATOR_RING)) return null;
            
            // check if ring is facing source pos (from)
            var candidateBent = candidateState.get(AcceleratorRingBlock.BENT);
            var candidateFacing = candidateState.get(Properties.HORIZONTAL_FACING);
            var candidateRedstone = candidateState.get(AcceleratorRingBlock.REDSTONE_STATE);
            
            var candidateBack = candidatePos.add(Geometry.getBackward(candidateFacing).multiply(i));
            var candidateFront = candidatePos.add(Geometry.getForward(candidateFacing).multiply(i));
            
            // front can be bent
            if (candidateBent == 1) candidateFront = candidateFront.add(Geometry.getLeft(candidateFacing).multiply(i));
            if (candidateBent == 2) candidateFront = candidateFront.add(Geometry.getRight(candidateFacing).multiply(i));
            
            var isValid = candidateBack.equals(from) || candidateFront.equals(from);
            
            // check if redstone input is valid
            if (!isValid && candidateRedstone != 3) {
                candidateFront = candidatePos.add(Geometry.getForward(candidateFacing).multiply(i));    // reset front
                if (candidateRedstone == 1) {
                    candidateFront = candidateFront.add(Geometry.getLeft(candidateFacing).multiply(i));
                } else if (candidateRedstone == 2) {
                    candidateFront = candidateFront.add(Geometry.getRight(candidateFacing).multiply(i));
                }
                
                isValid = candidateFront.equals(from);
            }
            
            if (isValid) return candidatePos;
            
        }
        
        
        return null;
        
    }
    
    // remove caches that have either source or target as pos
    public static void resetCachedGate(BlockPos pos) {
        var toRemove = cachedGates.entrySet().stream().filter(elem -> elem.getKey().getLeft().equals(pos) || elem.getValue().equals(pos)).map(Map.Entry::getKey).toList();
        toRemove.forEach(cachedGates::remove);
    }
    
    public void setDisplayTrail(List<Vec3d> displayTrail) {
        this.displayTrail = displayTrail;
    }
    
    public static final class CompPair<A, B> extends Pair<A, B> {
        
        public CompPair(A left, B right) {
            super(left, right);
        }
        
        @Override
        public int hashCode() {
            return (getLeft() == null ? 0 : getLeft().hashCode()) ^ (getRight() == null ? 0 : getRight().hashCode());
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CompPair<?, ?> p)) {
                return false;
            }
            
            return Objects.equals(p.getLeft(), getLeft()) && Objects.equals(p.getRight(), getRight());
        }
    }
    
    public static final class ActiveParticle {
        public Vec3d position;
        public float velocity;
        public BlockPos nextGate;
        public BlockPos lastGate;
        public float lastBendDistance = MAX_VELOCITY;
        public float lastBendDistance2 = MAX_VELOCITY;
        
        public ActiveParticle(Vec3d position, float velocity, BlockPos nextGate, BlockPos lastGate) {
            this.position = position;
            this.velocity = velocity;
            this.nextGate = nextGate;
            this.lastGate = lastGate;
        }
    }
    
}
