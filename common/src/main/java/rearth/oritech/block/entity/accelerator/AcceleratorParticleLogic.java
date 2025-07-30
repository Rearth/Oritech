package rearth.oritech.block.entity.accelerator;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.accelerator.AcceleratorPassthroughBlock;
import rearth.oritech.block.blocks.accelerator.AcceleratorRingBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// move this into a second class to keep the entity class smaller and focus on recipe handling, work interaction, etc.
public class AcceleratorParticleLogic {
    private final BlockPos pos;
    private final ServerLevel world;
    private final AcceleratorControllerBlockEntity entity;
    
    private static final Map<CompPair<BlockPos, Vec3i>, BlockPos> cachedGates = new HashMap<>();    // stores the next gate for a combo of source gate and direction
    private static final Map<BlockPos, BlockPos> activeParticles = new HashMap<>(); // stores relations between position of particle -> position of controller
    
    public AcceleratorParticleLogic(BlockPos pos, ServerLevel world, AcceleratorControllerBlockEntity entity) {
        this.pos = pos;
        this.world = world;
        this.entity = entity;
    }
    
    
    @SuppressWarnings("lossy-conversions")
    public void update(ActiveParticle particle) {
        
        var timePassed = 1 / 20f;
        
        var renderedTrail = new ArrayList<Vec3>();
        renderedTrail.add(particle.position);
        
        // list of positions this frame checked for entities
        var checkedPositions = new HashSet<BlockPos>();
        
        var availableDistance = particle.velocity * timePassed;
        while (availableDistance > 0.001) {
            
            if (particle.nextGate == null) {
                exitParticle(particle, new Vec3(0, 0, 0), AcceleratorControllerBlockEntity.ParticleEvent.ERROR);
                return;
            }
            
            var path = particle.nextGate.getCenter().subtract(particle.position);
            var pathLength = path.length();
            var moveDist = Math.min(pathLength, availableDistance);
            availableDistance -= moveDist;
            var movedBy = path.normalize().scale(moveDist);
            
            // check if old position intersects with another particle
            var abTest = movedBy.x > 0  || movedBy.y > 0;
            var validLastGate = particle.lastGate == null ? particle.nextGate : particle.lastGate;
            var usedGateForCollision = abTest ? validLastGate : particle.nextGate;
            
            if (updateParticleCollision(Vec3.atLowerCornerOf(usedGateForCollision), particle)) {
                return;
            }
            
            // update position
            particle.position = particle.position.add(movedBy);
            
            renderedTrail.add(particle.position);
            particle.lastBendDistance += moveDist;
            
            checkParticleEntityCollision(particle.position, particle, checkedPositions);
            
            if (moveDist >= pathLength - 0.1f) {
                // gate reached
                // calculate next gate direction
                var reachedGate = particle.nextGate;
                var nextDirection = getGateExitDirection(particle.lastGate, particle.nextGate);
                // try find next valid gate
                var nextGate = findNextGateCached(reachedGate, nextDirection, particle.velocity);
                
                // no gate built / too slow
                if (nextGate == null) {
                    exitParticle(particle, Vec3.atLowerCornerOf(nextDirection), AcceleratorControllerBlockEntity.ParticleEvent.EXITED_NO_GATE);
                    return;
                }
                
                // check if curve is too strong (based on reached gate)
                var gateOffset = particle.nextGate.subtract(particle.lastGate);
                var lastDirection = new Vec3i(Math.clamp(gateOffset.getX(), -1, 1), 0, Math.clamp(gateOffset.getZ(), -1, 1));
                var wasBend = !lastDirection.equals(nextDirection);
                if (wasBend) {
                    
                    var combinedDist = getParticleBendDist(particle.lastBendDistance, particle.lastBendDistance2);
                    var requiredDist = getRequiredBendDist(particle.velocity);
                    
                    if (combinedDist <= requiredDist) {
                        exitParticle(particle, Vec3.atLowerCornerOf(particle.nextGate.subtract(particle.lastGate)), AcceleratorControllerBlockEntity.ParticleEvent.EXITED_FAST);
                        return;
                    }
                    
                    particle.lastBendDistance2 = particle.lastBendDistance;
                    particle.lastBendDistance = 0;
                }
                
                // handle gate interaction (e.g. motor or sensor)
                var gateBlock = world.getBlockState(reachedGate).getBlock();
                if (gateBlock.equals(BlockContent.ACCELERATOR_MOTOR)) {
                    entity.handleParticleMotorInteraction(reachedGate);
                } else if (gateBlock.equals(BlockContent.ACCELERATOR_SENSOR) && world.getBlockEntity(reachedGate) instanceof AcceleratorSensorBlockEntity sensorEntity) {
                    sensorEntity.measureParticle(particle);
                }
                
                particle.nextGate = nextGate;
                particle.lastGate = reachedGate;
            }
        }
        
        entity.onParticleMoved(renderedTrail);
    }
    
    private void checkParticleEntityCollision(Vec3 position, ActiveParticle particle, Set<BlockPos> alreadyChecked) {
        
        var blockPos = BlockPos.containing(position);
        if (alreadyChecked.contains(blockPos)) return;
        alreadyChecked.add(blockPos);
        
        var targets = world.getEntitiesOfClass(LivingEntity.class, new AABB(blockPos), elem -> elem.isAlive() && elem.isAttackable() && !elem.isSpectator());
        var remainingMomentum = particle.velocity;
        for (var mob : targets) {
            var usedMomentum = entity.handleParticleEntityCollision(blockPos, particle, remainingMomentum, mob);
            remainingMomentum -= usedMomentum;
            
            if (remainingMomentum <= 0.1f) return;
        }
        
        particle.velocity = remainingMomentum;
    }
    
    private void exitParticle(ActiveParticle particle, Vec3 direction, AcceleratorControllerBlockEntity.ParticleEvent reason) {
        
        var exitFrom = particle.position;
        
        var distance = Math.max(Math.sqrt(particle.velocity), 0.4) * 0.9;
        var exitTo = exitFrom.add(direction.normalize().scale(distance));
        
        entity.onParticleExited(exitFrom, exitTo, particle.lastGate, direction, reason);
        
        var searchDist = (int) distance;
        var searchDirection = new Vec3i((int) Math.round(direction.x), 0, (int) Math.round(direction.z));
        var searchStart = particle.nextGate;
        if (searchStart == null) searchStart = particle.lastGate;
        
        var remainingMomentum = particle.velocity;
        
        for (int i = 1; i <= searchDist; i++) {
            var checkPos = searchStart.offset(searchDirection.multiply(i));
            
            var targets = world.getEntitiesOfClass(LivingEntity.class, new AABB(checkPos), elem -> elem.isAlive() && elem.isAttackable() && !elem.isSpectator());
            
            for (var mob : targets) {
                var usedMomentum = entity.handleParticleEntityCollision(checkPos, particle, remainingMomentum, mob);
                remainingMomentum -= usedMomentum;
                
                if (remainingMomentum <= 0.1f) return;
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
    
    private boolean updateParticleCollision(Vec3 position, ActiveParticle particle) {
        
        var blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
        if (activeParticles.containsKey(blockPos) && !activeParticles.get(blockPos).equals(this.pos)) {
            // found collision
            var secondControllerPos = activeParticles.get(blockPos);
            
            if (!(world.getBlockEntity(secondControllerPos) instanceof AcceleratorControllerBlockEntity secondAccelerator) || secondAccelerator.getParticle() == null)
                return false;
            
            var secondParticle = secondAccelerator.getParticle();
            var impactSpeed = particle.velocity + secondParticle.velocity;
            
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
        if (targetBlock.equals(BlockContent.ACCELERATOR_MOTOR) || targetBlock.equals(BlockContent.ACCELERATOR_SENSOR))
            return incomingDir;
        
        // if the target gate has just been destroyed
        if (!targetBlock.equals(BlockContent.ACCELERATOR_RING)) return incomingDir;
        
        var targetFacing = targetState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var targetBent = targetState.getValue(AcceleratorRingBlock.BENT);
        var targetRedstone = targetState.getValue(AcceleratorRingBlock.REDSTONE_STATE);
        
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
                return Geometry.getForward(targetFacing).offset(Geometry.getLeft(targetFacing));
            } else {   // bent right
                return Geometry.getForward(targetFacing).offset(Geometry.getRight(targetFacing));
            }
        }
        
    }
    
    public static float getMaxGateDist(float speed) {
        return (float) Math.clamp(Math.sqrt(speed) / 2, 2, Oritech.CONFIG.maxGateDist());
    }
    
    public static float getRequiredBendDist(float speed) {
        return (float) (Math.sqrt(speed) / Oritech.CONFIG.bendFactor());
    }
    
    public static float getParticleBendDist(float distA, float distB) {
        return distA + distB;
    }
    
    @Nullable
    private BlockPos findNextGateCached(BlockPos from, Vec3i direction, float speed) {
        
        var maxDist = getMaxGateDist(speed);
        var key = new CompPair<>(from, direction);
        
        if (cachedGates.containsKey(key)) {
            var result = cachedGates.get(key);
            var dist = (int) result.getCenter().distanceTo(from.getCenter());
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
        var maxDist = getMaxGateDist(speed);
        
        for (int i = 1; i <= maxDist; i++) {
            var candidatePos = from.offset(direction.multiply(i));
            var candidateState = world.getBlockState(candidatePos);
            if (candidateState.isAir()) continue;
            
            if (candidateState.getBlock().equals(BlockContent.ACCELERATOR_MOTOR) || candidateState.getBlock().equals(BlockContent.ACCELERATOR_SENSOR))
                return candidatePos;
            
            if (!candidateState.getBlock().equals(BlockContent.ACCELERATOR_RING)) return null;
            
            // check if ring is facing source pos (from)
            var candidateBent = candidateState.getValue(AcceleratorRingBlock.BENT);
            var candidateFacing = candidateState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            var candidateRedstone = candidateState.getValue(AcceleratorRingBlock.REDSTONE_STATE);
            
            var candidateBack = candidatePos.offset(Geometry.getBackward(candidateFacing).multiply(i));
            var candidateFront = candidatePos.offset(Geometry.getForward(candidateFacing).multiply(i));
            
            // front can be bent
            if (candidateBent == 1) candidateFront = candidateFront.offset(Geometry.getLeft(candidateFacing).multiply(i));
            if (candidateBent == 2) candidateFront = candidateFront.offset(Geometry.getRight(candidateFacing).multiply(i));
            
            var isValid = candidateBack.equals(from) || candidateFront.equals(from);
            
            // check if redstone input is valid
            if (!isValid && candidateRedstone != 3) {
                candidateFront = candidatePos.offset(Geometry.getForward(candidateFacing).multiply(i));    // reset front
                if (candidateRedstone == 1) {
                    candidateFront = candidateFront.offset(Geometry.getLeft(candidateFacing).multiply(i));
                } else if (candidateRedstone == 2) {
                    candidateFront = candidateFront.offset(Geometry.getRight(candidateFacing).multiply(i));
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
        var toRemove = cachedGates.entrySet().stream().filter(elem -> elem.getKey().getA().equals(pos) || elem.getValue().equals(pos)).map(Map.Entry::getKey).toList();
        toRemove.forEach(cachedGates::remove);
    }
    
    public static void resetNearbyCache(BlockPos pos) {
        var toRemove = cachedGates.keySet().stream().filter(blockPos -> blockPos.getA().distManhattan(pos) < Oritech.CONFIG.maxGateDist() + 1).toList();
        toRemove.forEach(cachedGates::remove);
    }
    
    public static final class CompPair<A, B> extends Tuple<A, B> {
        
        public CompPair(A left, B right) {
            super(left, right);
        }
        
        @Override
        public int hashCode() {
            return (getA() == null ? 0 : getA().hashCode()) ^ (getB() == null ? 0 : getB().hashCode());
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CompPair<?, ?> p)) {
                return false;
            }
            
            return Objects.equals(p.getA(), getA()) && Objects.equals(p.getB(), getB());
        }
    }
    
    public static final class ActiveParticle {
        public Vec3 position;
        public float velocity;
        public BlockPos nextGate;
        public BlockPos lastGate;
        public float lastBendDistance = 15000;
        public float lastBendDistance2 = 15000;
        
        public ActiveParticle(Vec3 position, float velocity, BlockPos nextGate, BlockPos lastGate) {
            this.position = position;
            this.velocity = velocity;
            this.nextGate = nextGate;
            this.lastGate = lastGate;
        }
    }
    
}
