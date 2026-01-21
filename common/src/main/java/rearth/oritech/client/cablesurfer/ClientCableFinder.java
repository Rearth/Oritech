package rearth.oritech.client.cablesurfer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.entity.interaction.PowerPoleEntity;

import java.util.ArrayList;

// yeah the raycast code is mostly from gemini
public class ClientCableFinder {
    
    private static final boolean DEBUG_DRAW = false;
    private static final int POLE_SEARCH_RADIUS = 196;
    
    /**
     * @param poleA Origin Pole
     * @param poleB Target Pole
     * @param selectedStart The specific Vec3 start point of the cable hit (Left or Right cable)
     * @param selectedEnd The specific Vec3 end point of the cable hit
     */
    public record CableHit(BlockPos poleA, BlockPos poleB, Vec3 selectedStart, Vec3 selectedEnd, Vec3 parallelStart, Vec3 parallelEnd) {}
    
    public static CableHit findLookedAtCable(Player player, float reachDistance) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        
        var eyePos = player.getEyePosition(1.0f);
        var lookDir = player.getViewVector(1.0f).normalize();
        
        var minPos = BlockPos.containing(eyePos.add(-POLE_SEARCH_RADIUS, -POLE_SEARCH_RADIUS, -POLE_SEARCH_RADIUS));
        var maxPos = BlockPos.containing(eyePos.add(POLE_SEARCH_RADIUS, POLE_SEARCH_RADIUS, POLE_SEARCH_RADIUS));
        var nearbyPoles = new ArrayList<PowerPoleEntity>();
        
        for (var cx = (minPos.getX() >> 4); cx <= (maxPos.getX() >> 4); cx++) {
            for (var cz = (minPos.getZ() >> 4); cz <= (maxPos.getZ() >> 4); cz++) {
                if (level.hasChunk(cx, cz)) {
                    for (var be : level.getChunk(cx, cz).getBlockEntities().values()) {
                        if (be instanceof PowerPoleEntity pole &&
                              player.distanceToSqr(Vec3.atCenterOf(pole.getBlockPos())) < POLE_SEARCH_RADIUS * POLE_SEARCH_RADIUS) {
                            nearbyPoles.add(pole);
                        }
                    }
                }
            }
        }
        
        var bestDistSq = Double.MAX_VALUE;
        CableHit bestHit = null;
        
        // Visual variables for debug
        Vec3 debugHitPos = null;
        
        for (var startPole : nearbyPoles) {
            var startPos = startPole.getBlockPos();
            var startCenter = Vec3.atCenterOf(startPos);
            // Replicate Renderer Geometry: Get offset vector
            var startFacing = startPole.getFacingForMultiblock();
            var startSideVec = Vec3.atLowerCornerOf(startFacing.getNormal()); // Assuming Geometry.getForward maps to this
            
            for (var target : startPole.getConnections()) {
                var endPos = target.pos();
                
                var endCenter = Vec3.atCenterOf(endPos);
                var endFacing = target.facing();
                var endSideVec = Vec3.atLowerCornerOf(endFacing.getNormal());
                
                if (!isPlayerNearCableBounds(player, startCenter, endCenter, reachDistance)) {
                    continue;
                }
                
                // crossing logic, needs to match renderer
                var startWorldA = startCenter.add(startSideVec);
                var startWorldB = startCenter.subtract(startSideVec);
                
                var targetWorldA = endCenter.add(endSideVec);
                var targetWorldB = endCenter.subtract(endSideVec);
                
                // calculate Cross vs Direct distance
                var distDirect = startWorldA.distanceToSqr(targetWorldA) + startWorldB.distanceToSqr(targetWorldB);
                var distCross = startWorldA.distanceToSqr(targetWorldB) + startWorldB.distanceToSqr(targetWorldA);
                
                // Define the two pairs of cables
                Vec3 cable1Start, cable1End;
                Vec3 cable2Start, cable2End;
                
                if (distDirect < distCross) {
                    // direct Connection
                    cable1Start = startWorldA; cable1End = targetWorldA;
                    cable2Start = startWorldB; cable2End = targetWorldB;
                } else {
                    // crossed Connection
                    cable1Start = startWorldA; cable1End = targetWorldB;
                    cable2Start = startWorldB; cable2End = targetWorldA;
                }
                
                
                // Check Cable 1
                var result1 = raycastCable(cable1Start, cable1End, eyePos, lookDir, reachDistance, level);
                if (result1 != null && result1.distSq < bestDistSq) {
                    bestDistSq = result1.distSq;
                    bestHit = new CableHit(startPos, endPos, cable1Start, cable1End, cable2Start, cable2End);
                    debugHitPos = result1.hitPos;
                }
                
                // Check Cable 2
                var result2 = raycastCable(cable2Start, cable2End, eyePos, lookDir, reachDistance, level);
                if (result2 != null && result2.distSq < bestDistSq) {
                    bestDistSq = result2.distSq;
                    bestHit = new CableHit(startPos, endPos, cable2Start, cable2End, cable1Start, cable1End);
                    debugHitPos = result2.hitPos;
                }
            }
        }
        
        if (DEBUG_DRAW && debugHitPos != null) {
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, debugHitPos.x, debugHitPos.y, debugHitPos.z, 0, 0, 0);
        }
        
        return bestHit;
    }
    
    private static boolean isPlayerNearCableBounds(Player player, Vec3 start, Vec3 end, float reach) {
        double minX = Math.min(start.x, end.x) - reach;
        double minY = Math.min(start.y, end.y) - reach - 10.0; // Extra Y buffer for Sag
        double minZ = Math.min(start.z, end.z) - reach;
        
        double maxX = Math.max(start.x, end.x) + reach;
        double maxY = Math.max(start.y, end.y) + reach;
        double maxZ = Math.max(start.z, end.z) + reach;
        
        Vec3 p = player.position();
        return p.x >= minX && p.x <= maxX &&
                 p.y >= minY && p.y <= maxY &&
                 p.z >= minZ && p.z <= maxZ;
    }
    
    private record RayResult(double distSq, Vec3 hitPos) {}
    
    private static RayResult raycastCable(Vec3 p1, Vec3 p2, Vec3 eyePos, Vec3 lookDir, float reach, net.minecraft.world.level.Level level) {
        double cableLength = p1.distanceTo(p2);
        int segments = Mth.clamp((int)(cableLength), 8, 128);
        
        double hitRadius = 1.5;
        double hitRadiusSq = hitRadius * hitRadius;
        double reachSq = reach * reach;
        
        var prevPoint = CableMath.getAt(p1, p2, 0);
        
        RayResult bestLocal = null;
        double bestLocalDist = Double.MAX_VALUE;
        
        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            var nextPoint = CableMath.getAt(p1, p2, t);
            
            // Debug: Draw the segments being calculated
            if (DEBUG_DRAW) {
                // Draw a flame every few ticks or segments to reduce lag,
                // or just draw points.
                    level.addParticle(ParticleTypes.FLAME, nextPoint.x, nextPoint.y, nextPoint.z, 0, 0, 0);
            }
            
            // Math: Closest point on this segment to the view ray
            var closestOnSeg = getClosestPointOnSegment(prevPoint, nextPoint, eyePos, lookDir);
            
            // 1. Line-Line distance check (Ray vs Segment)
            if (distSqPointToRay(closestOnSeg, eyePos, lookDir) < hitRadiusSq) {
                
                // 2. Reach check
                double distToPlayer = eyePos.distanceToSqr(closestOnSeg);
                if (distToPlayer < reachSq) {
                    if (distToPlayer < bestLocalDist) {
                        bestLocalDist = distToPlayer;
                        bestLocal = new RayResult(distToPlayer, closestOnSeg);
                    }
                }
            }
            prevPoint = nextPoint;
        }
        return bestLocal;
    }
    
    private static Vec3 getClosestPointOnSegment(Vec3 segA, Vec3 segB, Vec3 rayOrigin, Vec3 rayDir) {
        var u = rayDir;
        var v = segB.subtract(segA);
        var w = rayOrigin.subtract(segA);
        var a = u.dot(u);
        var b = u.dot(v);
        var c = v.dot(v);
        var d = u.dot(w);
        var e = v.dot(w);
        var D = a * c - b * b;
        double tc;
        if (D < 1e-8) tc = (b > c ? d / b : e / c);
        else tc = (a * e - b * d) / D;
        return segA.add(v.scale(Mth.clamp(tc, 0.0, 1.0)));
    }
    
    private static double distSqPointToRay(Vec3 point, Vec3 rayOrigin, Vec3 rayDir) {
        var w = point.subtract(rayOrigin);
        var proj = w.dot(rayDir);
        if (proj < 0) proj = 0;
        return point.distanceToSqr(rayOrigin.add(rayDir.scale(proj)));
    }
}