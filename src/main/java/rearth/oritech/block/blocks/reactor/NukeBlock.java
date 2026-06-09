package rearth.oritech.block.blocks.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class NukeBlock extends Block {

    private final boolean small;

    public NukeBlock(Properties settings, boolean small) {
        super(settings);
        this.small = small;
    }

    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (level.hasNeighborSignal(pos)) {
                primeTnt(level, pos);
            }

        }
    }

    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            primeTnt(level, pos);
        }

    }

    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide()) {
            primeTnt(level, pos);
        }
    }

    private void primeTnt(Level level, BlockPos pos) {
        if (!level.isClientSide()) {

            if (OritechConfig.boringNukes.get()) {
                var center = pos.getCenter();
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                level.explode(null, center.x, center.y, center.z, 3, true, Level.ExplosionInteraction.TNT);
                level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }

            var target = small ? BlockContent.REACTOR_EXPLOSION_MEDIUM : BlockContent.REACTOR_EXPLOSION_LARGE;
            level.setBlockAndUpdate(pos, target.defaultBlockState());
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        } else {
            primeTnt(level, pos);
            var item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                stack.consume(1, player);
            }

            player.awardStat(Stats.ITEM_USED.get(item));
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
    }

    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide()) {
            var blockPos = hit.getBlockPos();
            if (projectile.isOnFire() && projectile.mayInteract(level, blockPos)) {
                primeTnt(level, blockPos);
            }
        }

    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        super.addToTooltip(tooltipContext, consumer, tooltipFlag, dataComponentGetter)
        var key = small ? "block.oritech.low_yield_nuke.tooltip" : "block.oritech.nuke.tooltip";
        consumer.accept(Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        consumer.accept(Component.translatable(key + ".2").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
