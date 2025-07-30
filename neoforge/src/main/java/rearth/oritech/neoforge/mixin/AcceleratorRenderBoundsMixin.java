package rearth.oritech.neoforge.mixin;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.spongepowered.asm.mixin.Mixin;
import rearth.oritech.client.renderers.AcceleratorControllerRenderer;

@Mixin(AcceleratorControllerRenderer.class)
public class AcceleratorRenderBoundsMixin implements IBlockEntityRendererExtension {
    
    @Override
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
