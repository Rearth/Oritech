package rearth.oritech.api.accelerator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.entity.accelerator.ParticleAcceleratorBlockEntity;
import rearth.oritech.block.entity.accelerator.ParticleAcceleratorBlockEntity.ParticleEvent;

/**
 * Information about a particle hitting a block after leaving the accelerator ring.
 *
 * @param level         the level the accelerator is in
 * @param controllerPos the position of the particle accelerator controller
 * @param hitPos        the block the particle is hitting
 * @param hitState      the state of that block
 * @param direction     the exit direction of the particle
 * @param reason        why the particle left the ring (EXITED_NO_GATE or EXITED_FAST)
 * @param speed         the remaining momentum of the particle at this block
 * @param particleItem  a copy of the accelerated item, empty if an earlier behavior consumed it
 * @param accelerator   the accelerator block entity itself
 */
public record ParticleImpactContext(ServerLevel level, BlockPos controllerPos, BlockPos hitPos, BlockState hitState,
                                    Vec3 direction, ParticleEvent reason, long speed, ItemStack particleItem,
                                    ParticleAcceleratorBlockEntity accelerator) {
}
