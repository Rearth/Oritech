package rearth.oritech.block.blocks.processing;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RefineryBlock extends MultiblockMachine {

    public static Queue<Runnable> DELAYED_TAINT_EVENTS = new ArrayDeque<>();

    public RefineryBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return RefineryBlockEntity.class;
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        super.addToTooltip(tooltipContext, consumer, tooltipFlag, dataComponentGetter);

        var showExtra = Minecraft.getInstance().hasControlDown();

        if (showExtra) {
            consumer.accept(Component.translatable("tooltip.oritech.refinery_block").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {

        var refineryEntity = level.getBlockEntity(pos, BlockEntitiesContent.REFINERY_ENTITY.get());

        if (level.isClientSide() || refineryEntity.isEmpty()) {
            super.onExplosionHit(state, level, pos, explosion, onHit);
            return;
        }

        var crystalCandidate = refineryEntity.get().getNearbyNonEmptyCatalyst();
        if (crystalCandidate.isEmpty()) {
            super.onExplosionHit(state, level, pos, explosion, onHit);
            return;
        }

        refineryEntity.get().taintTransform();
        var color = refineryEntity.get().currentColor;

        // custom merger to void refinery self drop
        super.onExplosionHit(state, level, pos, explosion, ((itemStack, blockPos) -> {
        }));

        // explode crystal
        crystalCandidate.get().doExplosion();

        var targetPos = pos.immutable();

        // run in next tick to avoid explosion block weirdness
        DELAYED_TAINT_EVENTS.add(() -> {
            // create + init refinery
            level.setBlockAndUpdate(targetPos,
                    BlockContent.TAINTED_REFINERY_BLOCK.get().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING))
            );

            if (level.getBlockEntity(targetPos) instanceof TaintedRefineryBlockEntity taintedRefinery) {
                taintedRefinery.afterCreation();
                taintedRefinery.assignColor(color);
            }
        });

        // idea / potential todo: particles released from catalyst to refinery (along random offset paths?)
    }

    // todo maybe this can be cleaned up using server.execute or something similar()?
    public static void updateTaintEvents() {
        for (var elem : DELAYED_TAINT_EVENTS) {
            elem.run();
        }
        DELAYED_TAINT_EVENTS.clear();
    }
}
