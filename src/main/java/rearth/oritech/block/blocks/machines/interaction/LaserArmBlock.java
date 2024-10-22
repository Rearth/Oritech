package rearth.oritech.block.blocks.machines.interaction;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.behavior.LaserArmBlockBehavior;
import rearth.oritech.block.behavior.LaserArmEntityBehavior;
import rearth.oritech.block.entity.machines.interaction.LaserArmBlockEntity;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.MultiblockMachineController;

import java.util.List;
import java.util.Map;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;

public class LaserArmBlock extends Block implements BlockEntityProvider {

    private static final LaserArmBlockBehavior DEFAULT_BLOCK_BEHAVIOR = new LaserArmBlockBehavior();
    public static final Map<Block, LaserArmBlockBehavior> BLOCK_BEHAVIORS = new Object2ObjectOpenHashMap<>();
    private static final LaserArmEntityBehavior DEFAULT_ENTITY_BEHAVIOR = new LaserArmEntityBehavior();
    public static final Map<EntityType<?>, LaserArmEntityBehavior> ENTITY_BEHAVIORS = new Object2ObjectOpenHashMap<>();
    
    public LaserArmBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ASSEMBLED, false).with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        LaserArmBlockBehavior.registerDefaults();
        LaserArmEntityBehavior.registerDefaults();
    }

    public static void registerBlockBehavior(Block targetBlock, LaserArmBlockBehavior behavior) {
        BLOCK_BEHAVIORS.put(targetBlock, behavior);
    }

    public static void registerEntityBehavior(EntityType<?> entityType, LaserArmEntityBehavior behavior) {
        ENTITY_BEHAVIORS.put(entityType, behavior);
    }

    public static LaserArmBlockBehavior getBehaviorForBlock(Block targetBlock) {
        return BLOCK_BEHAVIORS.getOrDefault(targetBlock, DEFAULT_BLOCK_BEHAVIOR);
    }

    public static LaserArmEntityBehavior getBehaviorForEntity(EntityType<?> targetEntityType) {
        return ENTITY_BEHAVIORS.getOrDefault(targetEntityType, DEFAULT_ENTITY_BEHAVIOR);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ASSEMBLED);
        builder.add(Properties.HORIZONTAL_FACING);
    }
    
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }
    
    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        
        if (world.isClient) return;
        
        var isPowered = world.isReceivingRedstonePower(pos);
        
        var laserEntity = (LaserArmBlockEntity) world.getBlockEntity(pos);
        laserEntity.setRedstonePowered(isPowered);
        
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof LaserArmBlockEntity laserArm)) {
                return ActionResult.SUCCESS;
            }
            
            var wasAssembled = state.get(ASSEMBLED);
            
            if (!wasAssembled) {
                var corePlaced = laserArm.tryPlaceNextCore(player);
                if (corePlaced) return ActionResult.SUCCESS;
            }
            
            var isAssembled = laserArm.initMultiblock(state);
            
            // first time created
            if (isAssembled && !wasAssembled) {
                NetworkContent.MACHINE_CHANNEL.serverHandle(entity).send(new NetworkContent.MachineSetupEventPacket(pos));
                return ActionResult.SUCCESS;
            }
            
            if (!isAssembled) {
                player.sendMessage(Text.translatable("message.oritech.machine.missing_core"));
                return ActionResult.SUCCESS;
            }
            
            laserArm.initAddons();
            
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
            
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        
        if (!world.isClient()) {
            
            var entity = world.getBlockEntity(pos);
            if (state.get(ASSEMBLED) && entity instanceof MultiblockMachineController machineEntity) {
                machineEntity.onControllerBroken();
            }
            
            if (entity instanceof LaserArmBlockEntity storageBlock) {
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
        return new LaserArmBlockEntity(pos, state);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
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
