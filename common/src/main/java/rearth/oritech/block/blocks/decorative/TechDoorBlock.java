package rearth.oritech.block.blocks.decorative;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.decorative.TechDoorBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.SoundContent;

import java.util.Objects;

public class TechDoorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public static final BooleanProperty OPENED = BooleanProperty.create("open");
    public static final VoxelShape CLOSED_SHAPE_SOUTH = Block.box(0, 0, 11, 16, 16, 16);
    public static final VoxelShape CLOSED_SHAPE_WEST = Block.box(0, 0, 0, 5, 16, 16);
    public static final VoxelShape CLOSED_SHAPE_NORTH = Block.box(0, 0, 0, 16, 16, 5);
    public static final VoxelShape CLOSED_SHAPE_EAST = Block.box(11, 0, 0, 16, 16, 16);
    
    public TechDoorBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(OPENED, false).setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPENED);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClientSide) return;
        
        var isOpen = state.getValue(OPENED);
        var isPowered = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.above());
        if (isOpen == isPowered) return;

        var aboveState = world.getBlockState(pos.above());
        
        if (!aboveState.getBlock().equals(BlockContent.TECH_DOOR_HINGE)) return;
        
        var entity = (TechDoorBlockEntity) world.getBlockEntity(pos);
        
        if (entity.shouldPlaySoundAgain())
            world.playSound(null, pos, SoundContent.PRESS, SoundSource.BLOCKS, Oritech.CONFIG.machineVolumeMultiplier() * 0.18f, 1.3f);
        
        world.setBlockAndUpdate(pos, state.setValue(OPENED, isPowered));
        world.setBlockAndUpdate(pos.above(), aboveState.setValue(OPENED, isPowered));
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getClosedShape(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPENED))
            return Shapes.empty();
        return super.getCollisionShape(state, world, pos, context);
    }
    
    public static VoxelShape getClosedShape(Direction facing) {
        return switch (facing) {
            case NORTH -> CLOSED_SHAPE_NORTH;
            case EAST -> CLOSED_SHAPE_EAST;
            case SOUTH -> CLOSED_SHAPE_SOUTH;
            case WEST -> CLOSED_SHAPE_WEST;
            default -> Shapes.empty();
        };
    }
    
    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var belowState = world.getBlockState(pos.below());
        var aboveState = world.getBlockState(pos.above());
        var belowValid = belowState.isFaceSturdy(world, pos.below(), Direction.UP);
        var aboveValid = aboveState.is(Blocks.AIR) || aboveState.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "replaceable")));
        return belowValid && aboveValid;
    }
    
    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClientSide)
            world.setBlockAndUpdate(pos.above(), BlockContent.TECH_DOOR_HINGE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide)
            world.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TechDoorBlockEntity(pos, state);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
