package rearth.oritech.block.blocks.reactor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class NukeBlock extends Block {
    
    private final boolean small;
    
    public NukeBlock(Settings settings, boolean small) {
        super(settings);
        this.small = small;
    }
    
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            if (world.isReceivingRedstonePower(pos)) {
                primeTnt(world, pos);
            }
            
        }
    }
    
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isReceivingRedstonePower(pos)) {
            primeTnt(world, pos);
        }
        
    }
    
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        if (!world.isClient) {
            primeTnt(world, pos);
        }
    }
    
    private void primeTnt(World world, BlockPos pos) {
        if (!world.isClient) {
            
            if (Oritech.CONFIG.boringNukes()) {
                var center = pos.toCenterPos();
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                world.createExplosion(null, center.x, center.y, center.z, 3, true, World.ExplosionSourceType.TNT);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return;
            }
            
            var target = small ? BlockContent.REACTOR_EXPLOSION_MEDIUM : BlockContent.REACTOR_EXPLOSION_LARGE;
            world.setBlockState(pos, target.getDefaultState());
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
    
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(Items.FLINT_AND_STEEL) && !stack.isOf(Items.FIRE_CHARGE)) {
            return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
        } else {
            primeTnt(world, pos);
            var item = stack.getItem();
            if (stack.isOf(Items.FLINT_AND_STEEL)) {
                stack.damage(1, player, LivingEntity.getSlotForHand(hand));
            } else {
                stack.decrementUnlessCreative(1, player);
            }
            
            player.incrementStat(Stats.USED.getOrCreateStat(item));
            return ItemActionResult.success(world.isClient);
        }
    }
    
    protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        if (!world.isClient) {
            var blockPos = hit.getBlockPos();
            if (projectile.isOnFire() && projectile.canModifyAt(world, blockPos)) {
                primeTnt(world, blockPos);
            }
        }
        
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        var key = small ? "block.oritech.low_yield_nuke.tooltip" : "block.oritech.nuke.tooltip";
        tooltip.add(Text.translatable(key).formatted(Formatting.GOLD, Formatting.ITALIC));
        tooltip.add(Text.translatable(key + ".2").formatted(Formatting.GOLD, Formatting.ITALIC));
    }
}
