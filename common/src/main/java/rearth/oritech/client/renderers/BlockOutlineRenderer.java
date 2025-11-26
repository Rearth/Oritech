package rearth.oritech.client.renderers;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.augmenter.AugmentResearchStationBlock;
import rearth.oritech.block.blocks.processing.RefineryModuleBlock;
import rearth.oritech.block.blocks.storage.LargeStorageBlock;
import rearth.oritech.block.blocks.storage.SmallStorageBlock;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MultiblockMachineController;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;

public class BlockOutlineRenderer {
    public static void render(ClientLevel world, Camera camera, PoseStack matrixStack, MultiBufferSource consumer) {
        if (world == null) return;
        
        var client = Minecraft.getInstance();
        var player = client.player;
        if (player == null || player.isShiftKeyDown()) return;
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) return;
        
        var itemStack = player.getMainHandItem();
        var blockPos = ((BlockHitResult) client.hitResult).getBlockPos();
        
        if (Oritech.CONFIG.showMachinePreview()) {
            renderBlockPlacementPreviewOutline(world, camera, matrixStack, consumer, itemStack, player, blockPos);
            renderParticlePlacementHelper(world, camera, matrixStack, consumer, itemStack, player, blockPos);
        }
        
        renderPromethiumPickaxeOutline(world, camera, matrixStack, consumer, itemStack, player, blockPos);
    }
    
    private static void renderBlockPlacementPreviewOutline(ClientLevel world, Camera camera, PoseStack matrixStack, MultiBufferSource consumer, ItemStack itemStack, LocalPlayer player, BlockPos blockPos) {
        
        var hasBlockItem = itemStack.getItem() instanceof BlockItem || itemStack.getItem().equals(ItemContent.UNSTABLE_CONTAINER);
        
        if (!hasBlockItem) return;
        
        var block = itemStack.getItem() instanceof BlockItem ? ((BlockItem) itemStack.getItem()).getBlock() : BlockContent.UNSTABLE_CONTAINER;
        
        if (!(block instanceof EntityBlock entityProvider) || !block.defaultBlockState().hasProperty(MultiblockMachine.ASSEMBLED))
            return;
        
        var machinePos = blockPos.offset(((BlockHitResult) player.minecraft.hitResult).getDirection().getNormal());
        if (itemStack.getItem().equals(ItemContent.UNSTABLE_CONTAINER))
            machinePos = blockPos;
        var placementState = block.getStateForPlacement(new BlockPlaceContext(player, player.swingingArm, itemStack, (BlockHitResult) player.minecraft.hitResult));
        var entity = entityProvider.newBlockEntity(machinePos, placementState);
        if (!(entity instanceof MultiblockMachineController multiblockController)) return;
        
        if (itemStack.getItem().equals(ItemContent.UNSTABLE_CONTAINER)) {
            var blockState = world.getBlockState(machinePos);
            var isValid = blockState.is(TagContent.UNSTABLE_CONTAINER_SOURCES_LOW) || blockState.is(TagContent.UNSTABLE_CONTAINER_SOURCES_MEDIUM) || blockState.is(TagContent.UNSTABLE_CONTAINER_SOURCES_HIGH);
            if (!isValid) return;
        }
        
        var coreOffsets = multiblockController.getCorePositions();
        var machineFacing = getFacingFromState(placementState);
        
        if (block instanceof LargeStorageBlock) {    // the large block is weird
            machineFacing = player.getDirection().getOpposite();
        } else if (block instanceof AugmentResearchStationBlock) {
            machineFacing = player.getNearestViewDirection();
        } else if (!(block instanceof MultiblockMachine || block instanceof RefineryModuleBlock)) {
            machineFacing = machineFacing.getOpposite();
        }
        
        var fullList = new ArrayList<>(coreOffsets);
        fullList.add(Vec3i.ZERO);
        
        matrixStack.pushPose();
        var cameraPos = camera.getPosition();
        matrixStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
        matrixStack.translate(0.005f, 0.005f, 0.005f); // slight offset to avoid z fighting
        
        var shape = Shapes.block();
        for (var coreOffset : fullList) {
            var fixedOffset = new Vec3i(coreOffset.getX(), coreOffset.getY(), coreOffset.getZ());
            var worldOffset = Geometry.offsetToWorldPosition(machineFacing, fixedOffset, machinePos);
            shape = Shapes.or(shape, Shapes.box(worldOffset.getX(), worldOffset.getY(), worldOffset.getZ(), worldOffset.getX() + 1, worldOffset.getY() + 1, worldOffset.getZ() + 1));
        }
        
        LevelRenderer.renderShape(matrixStack, consumer.getBuffer(RenderType.lines()), shape, 0, 0, 0, 1f, 1f, 1f, 0.7F);
        matrixStack.popPose();
        
    }
    
    private static Direction getFacingFromState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(SmallStorageBlock.TARGET_DIR)) {
            return state.getValue(SmallStorageBlock.TARGET_DIR);
        }
        
        return Direction.NORTH;
    }
    
    private static void renderPromethiumPickaxeOutline(ClientLevel world, Camera camera, PoseStack matrixStack, MultiBufferSource consumer, ItemStack itemStack, LocalPlayer player, BlockPos blockPos) {
        if (!itemStack.is(ToolsContent.PROMETHIUM_PICKAXE)) return;
        
        var offsetBlocks = PromethiumPickaxeItem.getOffsetBlocks(world, player, blockPos);
        
        matrixStack.pushPose();
        var cameraPos = camera.getPosition();
        matrixStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
        
        for (var offsetPos : offsetBlocks) {
            var offsetState = world.getBlockState(offsetPos);
            var renderShape = offsetState.getShape(world, offsetPos);
            
            matrixStack.pushPose();
            matrixStack.translate(offsetPos.getX(), offsetPos.getY(), offsetPos.getZ());
            LevelRenderer.renderShape(matrixStack, consumer.getBuffer(RenderType.lines()), renderShape, 0, 0, 0, 0.0F, 0.0F, 0.0F, 0.35F);
            matrixStack.popPose();
        }
        
        matrixStack.popPose();
    }
    
    private static void renderParticlePlacementHelper(ClientLevel world, Camera camera, PoseStack matrixStack, MultiBufferSource consumer, ItemStack itemStack, LocalPlayer player, BlockPos blockPos) {
        
        var isRing = itemStack.is(BlockContent.ACCELERATOR_RING.asItem());
        var isMotor = itemStack.is(BlockContent.ACCELERATOR_MOTOR.asItem());
        
        if (!isRing && !isMotor) return;
        
        assert player.minecraft.hitResult != null;
        
        var facing = player.getDirection();
        var cameraPos = camera.getPosition();
        var blockHit = (BlockHitResult) player.minecraft.hitResult;
        var targetPos = Vec3.atLowerCornerOf(blockHit.getBlockPos().offset(blockHit.getDirection().getNormal()));
        
        if (isMotor)
            facing = facing.getClockWise();
        
        var shape = Shapes.box(7/16f, 7/16f, 0, 9/16f, 9/16f, 1f);
        var halfShape = Shapes.box(4/16f, 7/16f, 0.8, 6/16f, 9/16f, 1.3f);
        var halfShapeLeft = Shapes.box(8/16f, 7/16f, 0.3, 10/16f, 9/16f, 0.8f);
        
        matrixStack.pushPose();
        
        matrixStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
        matrixStack.translate(targetPos.x, targetPos.y, targetPos.z);
        matrixStack.translate(0.005f, 0.005f, 0.005f); // slight offset to avoid z fighting
        
        var rotationY = 0;
        var extraOffset = Vec3.ZERO;
        if (facing.equals(Direction.WEST)) {
            rotationY = 90;
            extraOffset = new Vec3(0, 0, 1);
        }
        if (facing.equals(Direction.SOUTH)) {
            rotationY = 180;
            extraOffset = new Vec3(1, 0, 1);
        }
        if (facing.equals(Direction.EAST)) {
            rotationY = 270;
            extraOffset = new Vec3(1, 0, 0);
        }
        
        
        matrixStack.translate(extraOffset.x, extraOffset.y, extraOffset.z);
        matrixStack.mulPose(Axis.YP.rotationDegrees(rotationY));
        
        LevelRenderer.renderShape(matrixStack, consumer.getBuffer(RenderType.lines()), shape, 0, 0, 0, 1F, 1.0F, 1.0F, 1F);
        
        if (isRing) {
            matrixStack.pushPose();
            matrixStack.mulPose(Axis.YP.rotationDegrees(30));
            LevelRenderer.renderShape(matrixStack, consumer.getBuffer(RenderType.lines()), halfShape, 0, 0, 0, 1F, 1.0F, 1.0F, 1F);
            matrixStack.popPose();
            
            matrixStack.pushPose();
            matrixStack.mulPose(Axis.YP.rotationDegrees(-30));
            LevelRenderer.renderShape(matrixStack, consumer.getBuffer(RenderType.lines()), halfShapeLeft, 0, 0, 0, 1F, 1.0F, 1.0F, 1F);
            matrixStack.popPose();
        }
        
        matrixStack.popPose();
    }
}
