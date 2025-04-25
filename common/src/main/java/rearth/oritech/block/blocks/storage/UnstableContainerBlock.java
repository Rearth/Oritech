package rearth.oritech.block.blocks.storage;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.storage.UnstableContainerBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class UnstableContainerBlock extends Block implements BlockEntityProvider {
    
    public static final BooleanProperty SETUP_DONE = BooleanProperty.of("setup");
    
    public UnstableContainerBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(ASSEMBLED, false).with(SETUP_DONE, false));
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ASSEMBLED);
        builder.add(SETUP_DONE);
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new UnstableContainerBlockEntity(pos, state);
    }
    
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        
        super.onPlaced(world, pos, state, placer, itemStack);
        
        if (world.isClient) {
            return;
        }
        
        var machineCandidate = world.getBlockEntity(pos, BlockEntitiesContent.UNSTABLE_CONTAINER_BLOCK_ENTITY);
        if (machineCandidate.isEmpty()) return;
        var machine = machineCandidate.get();
        var corePositions = machine.getCorePositions();
        
        for (var coreOffset : corePositions) {
            var coreWorldPos = pos.add(coreOffset);
            var coreState = world.getBlockState(coreWorldPos);
            if (!coreState.isAir()) {
                var breakingPlayer = placer instanceof PlayerEntity ? (PlayerEntity) placer : null;
                coreState.getBlock().onBreak(world, coreWorldPos, coreState, breakingPlayer);
                world.breakBlock(coreWorldPos, true, placer, 1);
            }
            world.setBlockState(coreWorldPos, BlockContent.MACHINE_CORE_HIDDEN.getDefaultState());
        }
        
        machine.initMultiblock(state);
        
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayerEntity) player, handler);
        }
        
        return ActionResult.SUCCESS;
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
}
