package rearth.oritech.block.blocks.machines.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.interaction.DronePortEntity;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.Objects;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class DronePortBlock extends Block implements BlockEntityProvider {
    
    public DronePortBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ASSEMBLED, false).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ASSEMBLED, Properties.HORIZONTAL_FACING);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof DronePortEntity dronePort)) {
                return ActionResult.SUCCESS;
            }
            
            var wasAssembled = state.get(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = dronePort.tryPlaceNextCore(player);
                if (corePlaced) return ActionResult.SUCCESS;
            }
            
            var isAssembled = dronePort.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                NetworkContent.MACHINE_CHANNEL.serverHandle(entity).send(new NetworkContent.MachineSetupEventPacket(pos));
                return ActionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendMessage(Text.literal("Machine is not assembled. Please add missing core blocks"));
                return ActionResult.SUCCESS;
            }
            
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient() && state.get(ASSEMBLED)) {
            
            var entity = world.getBlockEntity(pos);
            if (entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
        }
        
        return super.onBreak(world, pos, state, player);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DronePortEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
}
