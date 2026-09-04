package rearth.oritech.spaceage.simulation;

import net.minecraft.core.BlockPos;
import org.joml.Vector2i;

// Input for the legacy server flight. This stays separate from the programmable plan until that plan controls
// actual rockets rather than only producing a client preview.
public record RocketLaunchProfile(BlockPos worldStart, Vector2i targetOrbit) {
}
