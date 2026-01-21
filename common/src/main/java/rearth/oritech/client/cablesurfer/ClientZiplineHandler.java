package rearth.oritech.client.cablesurfer;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.init.ItemContent;
import rearth.oritech.util.ServerZiplineHandler;

public class ClientZiplineHandler {
    
    private static boolean active = false;
    private static Vec3 startPos;
    private static Vec3 endPos;
    private static Vec3 parallelStart;
    private static Vec3 parallelEnd;
    
    private static float progress;          // 0.0 (Start) to 1.0 (End)
    private static float currentSpeed;      // Blocks per tick
    private static double totalDistance;
    
    // Config
    public static final float HANG_OFFSET = 1.65f;   // Distance below the wire (Eye height + arm length)
    private static final float DRAG = 0.97f;         // Air resistance (slows you down if you release W)
    private static final float GRAVITY_FORCE = 0.1f;
    
    private static CameraType previousCamera;
    
    public static Vec3 getStartPos() {
        return startPos;
    }
    
    public static Vec3 getEndPos() {
        return endPos;
    }
    
    public static Vec3 getParallelStart() {
        return parallelStart;
    }
    
    public static Vec3 getParallelEnd() {
        return parallelEnd;
    }
    
    public static boolean isZiplining(Player player) {
        return AttachmentApi.getAttachmentValue(player, ServerZiplineHandler.ZIPLINING_STATE);
    }
    
    public static void start(Vec3 start, Vec3 end, Vec3 parStart, Vec3 parEnd, float initialSpeed) {
        if (active) return; // Already riding
        
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        
        active = true;
        startPos = start;
        endPos = end;
        totalDistance = start.distanceTo(end);
        parallelStart = parStart;
        parallelEnd = parEnd;
        
        var searchPos = player.position().add(0, HANG_OFFSET, 0);
        progress = calculateClosestProgress(start, end, searchPos);
        
        var cableDir = endPos.subtract(startPos).normalize();
        var lookDir = player.getLookAngle();
        
        currentSpeed = initialSpeed;
        
        // dot Product < 0 means looking opposite to cable direction
        if (cableDir.dot(lookDir) < 0) {
            currentSpeed = -initialSpeed;
        }
        if (currentSpeed == 0) currentSpeed = 0.15f;
        
        // force 3rd Person Camera
        previousCamera = Minecraft.getInstance().options.getCameraType();
        if (previousCamera == CameraType.FIRST_PERSON && Oritech.CONFIG.ziplineCameraSwitch()) {
            Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        
        // Initial Snap
        var ropePos = CableMath.getAt(startPos, endPos, progress);
        var initialPos = ropePos.add(0, -HANG_OFFSET, 0);
        player.setPos(initialPos.x, initialPos.y, initialPos.z);
        player.setDeltaMovement(player.getLookAngle().scale(currentSpeed));
    }
    
    private static float calculateClosestProgress(Vec3 start, Vec3 end, Vec3 targetPos) {
        var bestDistSq = Double.MAX_VALUE;
        var bestT = 0.0f;
        
        var steps = (int) start.distanceTo(end) + 1;
        
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            var pointOnWire = CableMath.getAt(start, end, t);
            
            double distSq = pointOnWire.distanceToSqr(targetPos);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestT = t;
            }
        }
        
        return bestT;
    }
    
    public static void onClientTick() {
        if (!active) return;
        
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive() || !player.getMainHandItem().is(ItemContent.WRENCH)) {
            dismount(false);
            return;
        }
        
        NetworkManager.sendToServer(new ServerZiplineHandler.ZiplinePlayerUsePacket());
        
        // Shift -> Drop
        if (player.input.shiftKeyDown) {
            dismount(false);
            return;
        }
        
        // Space -> Jump Off
        if (player.input.jumping) {
            dismount(true);
            return;
        }
        
        
        var directionMultiplier = 1;
        var cableDir = endPos.subtract(startPos).normalize();
        var lookDir = player.getLookAngle();
        
        // Dot Product < 0 means looking opposite to cable direction
        if (cableDir.dot(lookDir) < 0) {
            // Player is looking backwards. Swap start/end.
            directionMultiplier = -1;
        }
        
        var maxSpeed = Oritech.CONFIG.maxZiplineSpeed();
        var acceleration = Oritech.CONFIG.ziplineAcceleration();
        
        // W -> Accelerate
        if (player.input.up) {
            currentSpeed += acceleration * directionMultiplier;
        }
        // S -> Brake / Reverse
        else if (player.input.down) {
            currentSpeed -= acceleration * directionMultiplier;
        }
        
        // gravity calcs
        float t = progress;
        float tNext = Mth.clamp(t + 0.01f, 0.0f, 1.01f);
        var p1 = CableMath.getAt(startPos, endPos, t);
        var p2 = CableMath.getAt(startPos, endPos, tNext);
        Vec3 tangent = p2.subtract(p1).normalize();
        double slopeY = tangent.y; // -1 (Straight Down) to 1 (Straight Up)
        currentSpeed -= (float) (slopeY * GRAVITY_FORCE);
        
        // Apply Drag (Friction)
        currentSpeed *= DRAG;
        
        // Clamp Speed
        currentSpeed = Mth.clamp(currentSpeed, -maxSpeed, maxSpeed);
        
        // Convert speed (blocks/tick) to progress percentage (0.0-1.0)
        // distance = rate * time -> rate = distance / time (but here we step by speed)
        float progressDelta = (float) (currentSpeed / totalDistance);
        progress += progressDelta;
        
        // Check End of Line
        if (progress >= 1.0f) {
            progress = 1.0f;
            dismount(true); // Auto-jump at end
            return;
        } else if (progress <= 0.0f) {
            progress = 0.0f;
            currentSpeed = 0; // Hit the start, stop moving
            dismount(true);
        }
        
        // Calculate Position on Catenary Curve
        var ropePos = CableMath.getAt(startPos, endPos, progress);
        var nextPlayerPos = ropePos.add(0, -HANG_OFFSET, 0);
        
        // auto dismount near end
        if (Math.abs(currentSpeed) > 0.6 && Oritech.CONFIG.ziplineAutoJump()) {
            var blocksRemaining = (1.0f - progress) * totalDistance;
            if (directionMultiplier < 0) blocksRemaining = progress * totalDistance;
            
            // Calculate ejection distance dynamically
            // Formula: Minimum Buffer + (Speed * Ticks_Ahead)
            var dynamicEjectDist = 1.5 + (Math.abs(currentSpeed) * 3);
            
            if (blocksRemaining < dynamicEjectDist) {
                dismount(false);
                var currentVel = player.getDeltaMovement();
                player.setPos(nextPlayerPos.x, nextPlayerPos.y + 1, nextPlayerPos.z);
                player.setDeltaMovement(currentVel.x * 1.2, (currentVel.y > 0 ? currentVel.y * 1.2 : 0) + 0.7, currentVel.z * 1.2);
                playWooshSound(player);
                return;
            }
        }
        
        // We set the velocity to match the movement.
        // This ensures that if we dismount next tick, we carry the momentum.
        Vec3 oldPos = player.position();
        Vec3 velocity = nextPlayerPos.subtract(oldPos);
        
        var bounds = player.getDimensions(Pose.STANDING).makeBoundingBox(nextPlayerPos);
        bounds = bounds.deflate(0.1);
        
        if (!player.level().noCollision(player, bounds)) {
            dismount(false);
            return;
        }
        
        player.setPos(nextPlayerPos.x, nextPlayerPos.y, nextPlayerPos.z);
        player.setDeltaMovement(velocity);
        
        // Reset fall distance so we don't die on landing
        player.fallDistance = 0;
    }
    
    private static void dismount(boolean jump) {
        active = false;
        
        // Restore Camera
        if (previousCamera != null && Oritech.CONFIG.ziplineCameraSwitch()) {
            Minecraft.getInstance().options.setCameraType(previousCamera);
        }
        
        var player = Minecraft.getInstance().player;
        if (player != null) {
            if (jump) {
                // Add a little hop upwards + carry forward momentum
                Vec3 currentVel = player.getDeltaMovement();
                var playerPos = player.getPosition(0);
                player.setPos(playerPos.x, playerPos.y + 1, playerPos.z);
                player.setDeltaMovement(currentVel.x * 1.5, currentVel.y() * 1.2f + 0.6, currentVel.z * 1.5);
                playWooshSound(player);
            } else {
                // Just drop with current momentum
                player.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.5f);
            }
            
            var gustPos = player.getPosition(0);
            var random = player.level().random;
            var gustVel = player.getDeltaMovement();
            player.level().addParticle(
              ParticleTypes.GUST,
              gustPos.x, gustPos.y + 0.3, gustPos.z,
              gustVel.x + random.nextFloat() * 0.3, gustVel.y + random.nextFloat() * 0.3, gustVel.z + random.nextFloat() * 0.3
            );
        }
    }
    
    private static void playWooshSound(Player player) {
        player.playSound(SoundEvents.BAT_TAKEOFF, 2f, 2.0f);
    }
}