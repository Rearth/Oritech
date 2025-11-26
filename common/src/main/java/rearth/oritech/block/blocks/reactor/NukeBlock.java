package rearth.oritech.block.blocks.reactor;

import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;

import java.util.List;
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
import net.minecraft.world.phys.Vec3;

public class NukeBlock extends Block {
    
    private final boolean small;
    
    public NukeBlock(Properties settings, boolean small) {
        super(settings);
        this.small = small;
    }
    
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.is(state.getBlock())) {
            if (world.hasNeighborSignal(pos)) {
                primeTnt(world, pos);
            }
            
        }
    }
    
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.hasNeighborSignal(pos)) {
            primeTnt(world, pos);
        }
        
    }
    
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        if (!world.isClientSide) {
            primeTnt(world, pos);
        }
    }
    
    private void primeTnt(Level world, BlockPos pos) {
        if (!world.isClientSide) {
            
            if (Oritech.CONFIG.boringNukes()) {
                var center = pos.getCenter();
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                world.explode(null, center.x, center.y, center.z, 3, true, Level.ExplosionInteraction.TNT);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.0F, 1.0F);
                return;
            }
            
            var target = small ? BlockContent.REACTOR_EXPLOSION_MEDIUM : BlockContent.REACTOR_EXPLOSION_LARGE;
            world.setBlockAndUpdate(pos, target.defaultBlockState());
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
    
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        } else {
            primeTnt(world, pos);
            var item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                stack.consume(1, player);
            }
            
            player.awardStat(Stats.ITEM_USED.get(item));
            return ItemInteractionResult.sidedSuccess(world.isClientSide);
        }
    }
    
    protected void onProjectileHit(Level world, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!world.isClientSide) {
            var blockPos = hit.getBlockPos();
            if (projectile.isOnFire() && projectile.mayInteract(world, blockPos)) {
                primeTnt(world, blockPos);
            }
        }
        
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        var key = small ? "block.oritech.low_yield_nuke.tooltip" : "block.oritech.nuke.tooltip";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable(key + ".2").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }
}
