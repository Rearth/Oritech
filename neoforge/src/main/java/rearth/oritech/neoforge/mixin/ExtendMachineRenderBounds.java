package rearth.oritech.neoforge.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import rearth.oritech.client.renderers.AcceleratorControllerRenderer;
import rearth.oritech.client.renderers.LaserArmRenderer;
import rearth.oritech.client.renderers.MachineGantryRenderer;

// Self mixin to inject infinite render bounds to our own renderers.
// It would be better to have this directly in the renderers however AABB.INFINITE is a neoforge patch
// so that is not easily possible.

@Mixin({AcceleratorControllerRenderer.class, LaserArmRenderer.class, MachineGantryRenderer.class})
public class ExtendMachineRenderBounds<T extends BlockEntity> implements IBlockEntityRendererExtension<T> {
    @Override
    public @NotNull AABB getRenderBoundingBox(@NotNull BlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
