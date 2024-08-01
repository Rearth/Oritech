package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.decorative.TechDoorBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.SoundContent;

import java.util.Objects;

public class TechDoorBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    
    public static final BooleanProperty OPENED = BooleanProperty.of("open");
    public static final VoxelShape CLOSED_SHAPE_SOUTH = Block.createCuboidShape(0, 0, 11, 16, 16, 16);
    public static final VoxelShape CLOSED_SHAPE_WEST = Block.createCuboidShape(0, 0, 0, 5, 16, 16);
    public static final VoxelShape CLOSED_SHAPE_NORTH = Block.createCuboidShape(0, 0, 0, 16, 16, 5);
    public static final VoxelShape CLOSED_SHAPE_EAST = Block.createCuboidShape(11, 0, 0, 16, 16, 16);
    
    public TechDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OPENED, false).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPENED);
        builder.add(Properties.HORIZONTAL_FACING);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClient) return;
        
        var isOpen = state.get(OPENED);
        var isPowered = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
        if (isOpen == isPowered) return;

        var aboveState = world.getBlockState(pos.up());
        
        if (!aboveState.getBlock().equals(BlockContent.TECH_DOOR_HINGE)) return;
        
        var entity = (TechDoorBlockEntity) world.getBlockEntity(pos);
        
        if (entity.shouldPlaySoundAgain())
            world.playSound(null, pos, SoundContent.PRESS, SoundCategory.BLOCKS, Oritech.CONFIG.machineVolumeMultiplier() * 0.18f, 1.3f);
        
        world.setBlockState(pos, state.with(OPENED, isPowered));
        world.setBlockState(pos.up(), aboveState.with(OPENED, isPowered));
    }
    
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getClosedShape(state.get(Properties.HORIZONTAL_FACING));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(OPENED))
            return VoxelShapes.empty();
        return super.getCollisionShape(state, world, pos, context);
    }
    
    public static VoxelShape getClosedShape(Direction facing) {
        return switch (facing) {
            case NORTH -> CLOSED_SHAPE_NORTH;
            case EAST -> CLOSED_SHAPE_EAST;
            case SOUTH -> CLOSED_SHAPE_SOUTH;
            case WEST -> CLOSED_SHAPE_WEST;
            default -> VoxelShapes.empty();
        };
    }
    
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var belowState = world.getBlockState(pos.down());
        var aboveState = world.getBlockState(pos.up());
        var belowValid = belowState.isSideSolidFullSquare(world, pos.down(), Direction.UP);
        var aboveValid = aboveState.isOf(Blocks.AIR) || aboveState.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "replaceable")));
        return belowValid && aboveValid;
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient)
            world.setBlockState(pos.up(), BlockContent.TECH_DOOR_HINGE.getDefaultState().with(Properties.HORIZONTAL_FACING, state.get(Properties.HORIZONTAL_FACING)));
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient)
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState());
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TechDoorBlockEntity(pos, state);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
}
