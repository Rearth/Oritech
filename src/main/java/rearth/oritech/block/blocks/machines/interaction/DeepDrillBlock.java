package rearth.oritech.block.blocks.machines.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.interaction.DeepDrillEntity;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class DeepDrillBlock extends Block implements BlockEntityProvider {
    
    public DeepDrillBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ASSEMBLED, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ASSEMBLED);
    }
    
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof DeepDrillEntity deepDrill)) {
                return ActionResult.SUCCESS;
            }
            
            var wasAssembled = state.get(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = deepDrill.tryPlaceNextCore(player);
                if (corePlaced) return ActionResult.SUCCESS;
            }
            
            var isAssembled = deepDrill.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                NetworkContent.MACHINE_CHANNEL.serverHandle(entity).send(new NetworkContent.MachineSetupEventPacket(pos));
                return ActionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendMessage(Text.literal("Machine is not assembled. Please add missing core blocks"));
                return ActionResult.SUCCESS;
            }
            
            if (!deepDrill.init()) {
                player.sendMessage(Text.literal("Machine must be placed on ore nodes"));
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
            
            if (entity instanceof DeepDrillEntity storageBlock) {
                var stacks = storageBlock.inventory.heldStacks;
                for (var heldStack : stacks) {
                    if (!heldStack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                        world.spawnEntity(itemEntity);
                    }
                }
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
        return new DeepDrillEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        addMachineTooltip(tooltip, this, this);
    }
}
