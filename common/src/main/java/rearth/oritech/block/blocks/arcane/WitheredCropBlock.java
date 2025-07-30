package rearth.oritech.block.blocks.arcane;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import rearth.oritech.init.BlockContent;

import java.util.List;

public class WitheredCropBlock extends CropBlock {
    
    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[]{
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 5.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 10.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 13.0D, 16.0D),
      Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D)
    };
    
    public WitheredCropBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    protected ItemLike getBaseSeedId() {
        return BlockContent.WITHER_CROP_BLOCK;
    }
    
    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(Blocks.SOUL_SOIL);
    }
    
    @Override
    protected int getBonemealAgeIncrease(Level world) {
        return Mth.nextInt(world.random, 1, 2);
    }
    
    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (world.getRawBrightness(pos, 0) >= 9) {
            int age = this.getAge(state);
            if (age < this.getMaxAge()) {
                if (random.nextInt(5) == 0) {
                    world.setBlock(pos, this.getStateForAge(age + 1), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return AGE_TO_SHAPE[state.getValue(this.getAgeProperty())];
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (this.getAge(state) == this.getMaxAge())
            world.gameEvent(GameEvent.ENTITY_DIE.key(), pos, GameEvent.Context.of(state));
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        tooltip.add(Component.translatable("tooltip.oritech.soul_crop").withStyle(ChatFormatting.GRAY));
    }
}
