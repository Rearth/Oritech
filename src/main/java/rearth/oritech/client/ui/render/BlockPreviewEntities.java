package rearth.oritech.client.ui.render;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.generators.BigSolarPanelEntity;
import rearth.oritech.init.BlockContent;

/** Detached visual instances, never installed into or ticked by the world. */
public final class BlockPreviewEntities {

    private BlockPreviewEntities() {
    }

    public static @Nullable BlockEntity create(BlockPos pos, BlockState state, Level level) {

        var entity = state.is(BlockContent.BIG_SOLAR_PANEL) ? new SolarPreview(pos, state)
                : state.getBlock() instanceof EntityBlock block ? block.newBlockEntity(pos, state) : null;
        if (entity != null) entity.setLevel(level);
        return entity;
    }

    /** Space panels stay open and face the light independently of the Overworld clock. */
    private static final class SolarPreview extends BigSolarPanelEntity {

        private SolarPreview(BlockPos pos, BlockState state) {

            super(pos, state);
        }

        @Override
        public long getAdjustedTimeOfDay() {

            return 6000;
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

            controllers.add(new AnimationController<BigSolarPanelEntity>("preview", state -> state.setAndContinue(
                    getBlockState().getValue(MultiblockMachine.ASSEMBLED) ? MachineBlockEntity.IDLE : MachineBlockEntity.PACKAGED)));
        }
    }
}
