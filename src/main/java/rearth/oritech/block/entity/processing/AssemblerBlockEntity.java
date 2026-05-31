package rearth.oritech.block.entity.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.Geometry;

import java.util.List;

public class AssemblerBlockEntity extends MultiblockMachineEntity {

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ASSEMBLER_ENTITY.get(), pos, state, OritechConfig.processingMachines.assemblerData.energyPerTick.get());
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.assemblerData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.assemblerData.maxEnergyInsertion.get();
    }

    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.ASSEMBLER.get();
    }

    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 4, 4, 1);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 38, 26),
                new GuiSlot(1, 56, 26),
                new GuiSlot(2, 38, 44),
                new GuiSlot(3, 56, 44),
                new GuiSlot(4, 117, 36, true));
    }

    @Override
    protected void useEnergy() {
        super.useEnergy();

        if (level.random.nextFloat() > 0.4) return;
        // emit particles
        var facing = getFacing();
        var offsetLocal = Geometry.rotatePosition(new Vec3(0, 0.6, 0.5), facing);
        var emitPosition = Vec3.atCenterOf(worldPosition).add(offsetLocal);

        if (level instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT, emitPosition.x, emitPosition.y, emitPosition.z, 1, 0.6, 0.6, 0.6, 0);

    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.ASSEMBLER_SCREEN.get();
    }

    @Override
    public int getInventorySize() {
        return 5;
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
                new Vec3i(0, 0, 1),
                new Vec3i(0, 1, 0),
                new Vec3i(0, 1, 1)
        );
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, 2),
                new Vec3i(1, 0, 0)
        );
    }
}
