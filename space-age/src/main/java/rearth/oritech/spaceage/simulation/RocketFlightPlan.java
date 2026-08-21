package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.joml.Vector2i;

public record RocketFlightPlan(BlockPos worldStart, Vector2i targetOrbit) {
}
