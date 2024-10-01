package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.blocks.machines.accelerator.AcceleratorPassthroughBlock;
import rearth.oritech.block.blocks.machines.accelerator.AcceleratorRingBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// move this into a second class to keep the entity class smaller and focus on recipe handling, work interaction, etc.
public class AcceleratorParticleLogic {
    
    private static final int MAX_VELOCITY = 15000;
    private final BlockPos pos;
    private final ServerWorld world;
    private final AcceleratorControllerBlockEntity entity;
    
    private static final Map<CompPair<BlockPos, Vec3i>, BlockPos> cachedGates = new HashMap<>();    // stores the next gate for a combo of source gate and direction
    private static final Map<BlockPos, BlockPos> activeParticles = new HashMap<>(); // stores relations between position of particle -> position of controller
    
    public AcceleratorParticleLogic(BlockPos pos, ServerWorld world, AcceleratorControllerBlockEntity entity) {
        this.pos = pos;
        this.world = world;
        this.entity = entity;
    }
    
    
    @SuppressWarnings("lossy-conversions")
    public void update(ActiveParticle particle) {
        
        var timePassed = 1 / 20f;
        
        var renderedTrail = new ArrayList<Vec3d>();
        renderedTrail.add(particle.position);
        
        var availableDistance = particle.velocity * timePassed;
        while (availableDistance > 0.001) {
            
            if (particle.nextGate == null) {
                exitParticle(particle, new Vec3d(0, 0, 0));
                return;
            }
            
            var path = particle.nextGate.toCenterPos().subtract(particle.position);
            var pathLength = path.length();
            var moveDist = Math.min(pathLength, availableDistance);
            availableDistance -= moveDist;
            var movedBy = path.normalize().multiply(moveDist);
            
            // check if old position intersects with another particle
            if (updateParticleCollision(particle.position, particle)) {
                return;
            }
            
            // update position
            particle.position = particle.position.add(movedBy);
            
            renderedTrail.add(particle.position);
            particle.lastBendDistance += moveDist;
            
            // check if new position intersects with another particle
            if (updateParticleCollision(particle.position, particle)) {
                return;
            }
            
            if (moveDist >= pathLength - 0.1f) {
                // gate reached
                // calculate next gate direction
                var reachedGate = particle.nextGate;
                var nextDirection = getGateExitDirection(particle.lastGate, particle.nextGate);
                // try find next valid gate
                var nextGate = findNextGateCached(reachedGate, nextDirection, particle.velocity);
                
                // no gate built / too slow
                if (nextGate == null) {
                    exitParticle(particle, Vec3d.of(nextDirection));
                    return;
                }
                
                // check if curve is too strong (based on reached gate)
                var gateOffset = particle.nextGate.subtract(particle.lastGate);
                var lastDirection = new Vec3i(Math.clamp(gateOffset.getX(), -1, 1), 0, Math.clamp(gateOffset.getZ(), -1, 1));
                var wasBend = !lastDirection.equals(nextDirection);
                if (wasBend) {
                    
                    var combinedDist = particle.lastBendDistance + particle.lastBendDistance2;
                    
                    var requiredDist = Math.sqrt(particle.velocity) / 3;
                    if (combinedDist <= requiredDist) {
                        System.out.println("too fast! speed: " + particle.velocity + " at bend dist " + combinedDist);
                        exitParticle(particle, Vec3d.of(particle.nextGate.subtract(particle.lastGate)));
                        return;
                    }
                    
                    particle.lastBendDistance2 = particle.lastBendDistance;
                    particle.lastBendDistance = 0;
                }
                
                // handle gate interaction (e.g. motor or sensor)
                var gateBlock = world.getBlockState(reachedGate).getBlock();
                if (gateBlock.equals(BlockContent.ACCELERATOR_MOTOR)) {
                    particle.velocity += 1;
                } else if (gateBlock.equals(BlockContent.ACCELERATOR_SENSOR) && world.getBlockEntity(reachedGate) instanceof AcceleratorSensorBlockEntity sensorEntity) {
                    sensorEntity.measureParticle(particle);
                }
                
                particle.nextGate = nextGate;
                particle.lastGate = reachedGate;
            }
        }
        
        entity.onParticleMoved(renderedTrail);
    }
    
    private void exitParticle(ActiveParticle particle, Vec3d direction) {
        
        var exitFrom = particle.position;
        
        var distance = Math.max(Math.sqrt(particle.velocity), 0.9) * 0.9;
        var exitTo = exitFrom.add(direction.multiply(distance));
        
        entity.onParticleExited(exitFrom, exitTo, particle.lastGate, direction);
        
        var searchDist = (int) distance;
        var searchDirection = new Vec3i((int) Math.round(direction.x), 0, (int) Math.round(direction.z));
        var searchStart = particle.nextGate;
        if (searchStart == null) searchStart = particle.lastGate;
        
        var remainingMomentum = particle.velocity;
        
        for (int i = 1; i <= searchDist; i++) {
            var checkPos = searchStart.add(searchDirection.multiply(i));
            
            var targets = world.getEntitiesByClass(LivingEntity.class, new Box(checkPos), elem -> elem.isAlive() && elem.isAttackable() && !elem.isSpectator());
            
            if (!targets.isEmpty()) {
                for (var mob : targets) {
                    var usedMomentum = entity.handleParticleEntityCollision(checkPos, particle, remainingMomentum, mob);
                    remainingMomentum -= usedMomentum;
                    
                    if (remainingMomentum <= 0.1f) return;
                }
            }
            
            var block = world.getBlockState(checkPos);
            var targetableBlock = !block.isAir() && !(block.getBlock() instanceof AcceleratorPassthroughBlock);
            if (targetableBlock) {
                var usedMomentum = entity.handleParticleBlockCollision(checkPos, particle, remainingMomentum, block);
                remainingMomentum -= usedMomentum;
                
                if (remainingMomentum <= 0.1f) return;
            }
            
        }
        
    }
    
    private boolean updateParticleCollision(Vec3d position, ActiveParticle particle) {
        
        var blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
        if (activeParticles.containsKey(blockPos) && !activeParticles.get(blockPos).equals(this.pos)) {
            // found collision
            var secondControllerPos = activeParticles.get(blockPos);
            
            if (!(world.getBlockEntity(secondControllerPos) instanceof AcceleratorControllerBlockEntity secondAccelerator) || secondAccelerator.getParticle() == null)
                return false;
            
            var secondParticle = secondAccelerator.getParticle();
            var ownVelocity = particle.nextGate.toCenterPos().subtract(particle.lastGate.toCenterPos()).multiply(particle.velocity);
            var secondVelocity = secondParticle.nextGate.toCenterPos().subtract(secondParticle.lastGate.toCenterPos()).multiply(secondParticle.velocity);
            var impactSpeed = ownVelocity.distanceTo(secondVelocity);
            
            System.out.println("speeds: " + ownVelocity + " | " + secondVelocity + " | " + impactSpeed);
            entity.onParticleCollided((float) impactSpeed, particle.position, secondControllerPos, secondAccelerator);
            
            return true;
        }
        
        activeParticles.put(blockPos, this.pos);
        return false;
        
    }
    
    // this assumes the next gate is a valid target for a particle coming from lastGate.
    // Returns a neighboring or diagonal direction
    private Vec3i getGateExitDirection(BlockPos lastGate, BlockPos nextGate) {
        
        var incomingPath = nextGate.subtract(lastGate);
        var incomingStraight = incomingPath.getX() == 0 || incomingPath.getZ() == 0;
        var incomingDir = new Vec3i(Math.clamp(incomingPath.getX(), -1, 1), 0, Math.clamp(incomingPath.getZ(), -1, 1));
        
        var targetState = world.getBlockState(nextGate);
        var targetBlock = targetState.getBlock();
        
        // go straight through motors and sensors
        if (targetBlock.equals(BlockContent.ACCELERATOR_MOTOR) || targetBlock.equals(BlockContent.ACCELERATOR_SENSOR)) return incomingDir;
        
        // if the target gate has just been destroyed
        if (!targetBlock.equals(BlockContent.ACCELERATOR_RING)) return incomingDir;
        
        var targetFacing = targetState.get(net.minecraft.state.property.Properties.HORIZONTAL_FACING);
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
        }
        
        var candidate = findNextGate(from, direction, speed);
        if (candidate != null) {
            cachedGates.put(key, candidate);
        }
        
        return candidate;
        
    }
    
    // tries to find the next gate candidate, based on the starting gate
    // direction can be either straight or diagonal
    @Nullable
    public BlockPos findNextGate(BlockPos from, Vec3i direction, float speed) {
        
        // longer empty areas only work at higher speeds
        var maxDist = Math.clamp(Math.sqrt(speed), 2, 8);
        
        for (int i = 1; i <= maxDist; i++) {
            var candidatePos = from.add(direction.multiply(i));
            var candidateState = world.getBlockState(candidatePos);
            if (candidateState.isAir()) continue;
            
            if (candidateState.getBlock().equals(BlockContent.ACCELERATOR_MOTOR) || candidateState.getBlock().equals(BlockContent.ACCELERATOR_SENSOR)) return candidatePos;
            
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
    
    // called on server tick end. Used for collision detection
    public static void onTickEnd() {
        activeParticles.clear();
    }
    
    // remove caches that have either source or target as pos. Called from gate blocks
    public static void resetCachedGate(BlockPos pos) {
        var toRemove = cachedGates.entrySet().stream().filter(elem -> elem.getKey().getLeft().equals(pos) || elem.getValue().equals(pos)).map(Map.Entry::getKey).toList();
        toRemove.forEach(cachedGates::remove);
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
