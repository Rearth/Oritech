package rearth.oritech.block.blocks.augmenter;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.OritechClient;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;

import rearth.oritech.util.Geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;
import static rearth.oritech.util.TooltipHelper.addMachineTooltip;


public class AugmentApplicationBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    private final VoxelShape[] HITBOXES = computeShapes();
    private final HashMap<Player, Long> lastContact = new HashMap<>();
    
    public static Tuple<Long, Player> lastTeleportedPlayer;    // used to skip inv opening if a player just teleported in
    
    public AugmentApplicationBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(ASSEMBLED, false));
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        
        if (!state.getValue(ASSEMBLED)) {
            return super.getShape(state, world, pos, context);
        }
        
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return HITBOXES[facing.ordinal()];
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, ASSEMBLED);
    }
    
    private VoxelShape[] computeShapes() {
        
        var result = new VoxelShape[6];
        
        for (var facing : BlockStateProperties.HORIZONTAL_FACING.getPossibleValues()) {
            
            result[facing.ordinal()] = Shapes.or(
              Geometry.rotateVoxelShape(Shapes.box(0, 0, 0, 1, 2 / 16f, 1), facing, AttachFace.FLOOR),
              Geometry.rotateVoxelShape(Shapes.box(0, 3 / 16f, 14 / 16f, 1f, 1f, 1f), facing, AttachFace.FLOOR)
            );
        }
        
        return result;
        
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return Objects.requireNonNull(super.getStateForPlacement(ctx)).setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }
    
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return null;
    }
    
    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        
        if (world.isClientSide || !state.getValue(ASSEMBLED)) return;
        
        if (!(entity instanceof Player player)) return;
        
        if (lastTeleportedPlayer != null) {
            var age = world.getGameTime() - lastTeleportedPlayer.getA();
            if (age < 20) {
                return;
            }
        }
        
        var centerPos = pos.getBottomCenter().add(0, 0.2, 0);
        
        var dist = entity.position().distanceTo(centerPos);
        
        if (dist < 0.45) {
            
            var ageWithoutContact = world.getGameTime() - lastContact.getOrDefault(player, 0L);
            
            var time = world.getGameTime();
            lastContact.put(player, time);
            
            if (ageWithoutContact > 15) {
                var locked = lockPlayer(player, centerPos, state);
                if (locked) {
                    var blockEntity = (AugmentApplicationEntity) world.getBlockEntity(pos);
                    blockEntity.loadAvailableStations(player);
                    
                    var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                    MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
                }
            }
        }
        
    }
    
    private boolean lockPlayer(Player player, Vec3 lockPos, BlockState state) {
        
        var maxVelocity = Math.max(Math.max(Math.abs(player.getDeltaMovement().x), Math.abs(player.getDeltaMovement().y)), Math.abs(player.getDeltaMovement().z));
        
        if (maxVelocity < 0.01 || player.containerMenu instanceof PlayerModifierScreenHandler) return false;
        
        player.level().playSound(null, player.blockPosition(), SoundEvents.AXE_STRIP, SoundSource.BLOCKS);
        
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        var rotation = switch (facing) {
            case NORTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo((ServerLevel) player.level(), lockPos.x, lockPos.y, lockPos.z, Set.of(), rotation, 0);
        
        var dist = player.position().distanceTo(lockPos);
        
        return dist < 0.1;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (world.isClientSide)
            return InteractionResult.SUCCESS;
        
        var entity = world.getBlockEntity(pos);
        if (!(entity instanceof AugmentApplicationEntity modifierEntity)) {
            return InteractionResult.SUCCESS;
        }
        
        var wasAssembled = state.getValue(ASSEMBLED);
        
        if (!wasAssembled) {
            var corePlaced = modifierEntity.tryPlaceNextCore(player);
            if (corePlaced) return InteractionResult.SUCCESS;
        }
        
        var isAssembled = modifierEntity.initMultiblock(state);
        
        // first time created
        if (isAssembled && !wasAssembled) {
            modifierEntity.triggerSetupAnimation();
            return InteractionResult.SUCCESS;
        }
        
        if (!isAssembled) {
            player.sendSystemMessage(Component.translatable("message.oritech.machine.missing_core"));
            return InteractionResult.SUCCESS;
        }
        
        var blockEntity = (AugmentApplicationEntity) world.getBlockEntity(pos);
        blockEntity.loadAvailableStations(player);
        
        var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
        MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        
        if (!world.isClientSide()) {
            
            var entity = world.getBlockEntity(pos);
            
            if (entity instanceof AugmentApplicationEntity storageBlock) {
                storageBlock.onControllerBroken();
                var stacks = storageBlock.inventory.heldStacks;
                for (var heldStack : stacks) {
                    if (!heldStack.isEmpty()) {
                        var itemEntity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), heldStack);
                        world.addFreshEntity(itemEntity);
                    }
                }
                
                storageBlock.inventory.heldStacks.clear();
                storageBlock.inventory.setChanged();
            }
        }
        
        return super.playerWillDestroy(world, pos, state, player);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AugmentApplicationEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof BlockEntityTicker ticker)
                ticker.tick(world1, pos, state1, blockEntity);
        };
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        super.appendHoverText(stack, context, tooltip, options);
        var hotkey = OritechClient.AUGMENT_SELECTOR.key.getDisplayName();
        tooltip.add(Component.translatable("tooltip.oritech.augmenter.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.oritech.augmenter.2", hotkey.tryCollapseToString()).withStyle(ChatFormatting.GRAY));
        addMachineTooltip(tooltip, this, this);
    }
}
