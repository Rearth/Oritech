package rearth.oritech.client.cablesurfer;

import net.minecraft.world.phys.Vec3;

public class CableMath {
    public static Vec3 getAt(Vec3 start, Vec3 end, float t) {
        Vec3 totalOffset = end.subtract(start);
        double totalLength = totalOffset.length();
        double sag = Math.min(totalLength * 0.05f, 4);
        
        Vec3 currentPos = start.add(totalOffset.scale(t));
        double sagY = -sag * 4 * t * (1 - t);
        
        return currentPos.add(0, sagY, 0);
    }
}