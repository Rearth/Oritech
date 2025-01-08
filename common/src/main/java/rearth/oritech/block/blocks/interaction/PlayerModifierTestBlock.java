package rearth.oritech.block.blocks.interaction;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.PlayerModifierTestEntity;
import rearth.oritech.client.ui.PlayerModifierScreenHandler;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.Geometry;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import static rearth.oritech.block.base.block.MultiblockMachine.ASSEMBLED;

public class PlayerModifierTestBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    
    private final VoxelShape[] HITBOXES = computeShapes();
    private final HashMap<PlayerEntity, Long> lastContact = new HashMap<>();
    
    public PlayerModifierTestBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(ASSEMBLED, false));
    }
    
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        
        if (!state.get(ASSEMBLED)) {
            return super.getOutlineShape(state, world, pos, context);
        }
        
        var facing = state.get(Properties.HORIZONTAL_FACING);
        return HITBOXES[facing.ordinal()];
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, ASSEMBLED);
    }
    
    private VoxelShape[] computeShapes() {
        
        var result = new VoxelShape[6];
        
        for (var facing : Properties.HORIZONTAL_FACING.getValues()) {
            
            result[facing.ordinal()] = VoxelShapes.union(
              Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 0, 0, 1, 2/16f, 1), facing, BlockFace.FLOOR),
              Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 3/16f, 0, 2/16f, 1, 14/16f), facing, BlockFace.FLOOR),
              Geometry.rotateVoxelShape(VoxelShapes.cuboid(14/16f, 3/16f, 0, 1f, 1, 14/16f), facing, BlockFace.FLOOR),
              Geometry.rotateVoxelShape(VoxelShapes.cuboid(0, 3/16f, 14/16f, 1f, 1f, 1f), facing, BlockFace.FLOOR)
            );
        }
        
        return result;
        
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }
    
    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return null;
    }
    
    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        
        if (world.isClient || !state.get(ASSEMBLED)) return;
        
        if (!(entity instanceof PlayerEntity player)) return;
        
        var centerPos = pos.toBottomCenterPos().add(0, 0.2, 0);
        
        var dist = entity.getPos().distanceTo(centerPos);
        
        if (dist < 0.45) {
            
            var ageWithoutContact = world.getTime() - lastContact.getOrDefault(player, 0L);
            
            var time = world.getTime();
            lastContact.put(player, time);
            
            if (ageWithoutContact > 15) {
                var locked = lockPlayer(player, centerPos, state);
                if (locked) {
                    var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
                    player.openHandledScreen(handler);
                }
            }
        }
        
    }
    
    private boolean lockPlayer(PlayerEntity player, Vec3d lockPos, BlockState state) {
        
        var maxVelocity = Math.max(Math.max(Math.abs(player.getVelocity().x), Math.abs(player.getVelocity().y)), Math.abs(player.getVelocity().z));
        
        if (maxVelocity < 0.01 || player.currentScreenHandler instanceof PlayerModifierScreenHandler) return false;
        
        var facing = state.get(Properties.HORIZONTAL_FACING);
        var rotation = switch (facing) {
            case NORTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        player.setVelocity(Vec3d.ZERO);
        player.teleport((ServerWorld) player.getWorld(), lockPos.x, lockPos.y, lockPos.z, Set.of(), rotation, 0);
        
        var dist = player.getPos().distanceTo(lockPos);
        
        return dist < 0.1;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (world.isClient)
            return ActionResult.SUCCESS;
        
        var entity = world.getBlockEntity(pos);
        if (!(entity instanceof PlayerModifierTestEntity modifierEntity)) {
            return ActionResult.SUCCESS;
        }
        
        var wasAssembled = state.get(ASSEMBLED);
        
        if (!wasAssembled) {
            var corePlaced = modifierEntity.tryPlaceNextCore(player);
            if (corePlaced) return ActionResult.SUCCESS;
        }
        
        var isAssembled = modifierEntity.initMultiblock(state);
        
        // first time created
        if (isAssembled && !wasAssembled) {
            NetworkContent.MACHINE_CHANNEL.serverHandle(entity).send(new NetworkContent.MachineSetupEventPacket(pos));
            return ActionResult.SUCCESS;
        }
        
        if (!isAssembled) {
            player.sendMessage(Text.translatable("message.oritech.machine.missing_core"));
            return ActionResult.SUCCESS;
        }
        
        
        modifierEntity.onUse(player, state);
        
        return ActionResult.SUCCESS;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerModifierTestEntity(pos, state);
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
