package rearth.oritech.client.renderers;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import org.joml.Quaternionf;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.augmenter.AugmentResearchStationBlock;
import rearth.oritech.block.blocks.processing.RefineryModuleBlock;
import rearth.oritech.block.blocks.storage.LargeStorageBlock;
import rearth.oritech.block.blocks.storage.SmallStorageBlock;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.ToolsContent;
import rearth.oritech.init.datamap.DataMapContent;
import rearth.oritech.item.tools.harvesting.PromethiumPickaxeItem;
import rearth.oritech.util.ColorHelper;
import rearth.oritech.util.Geometry;
import rearth.oritech.util.MultiblockMachineController;

import java.util.ArrayList;
import java.util.HashSet;

public class BlockOutlineRenderer {

    // pos is in world pos. offset (in block space) and rotation (around the block origin) are applied after
    // translating to pos but before rendering the shape, allowing non axis-aligned previews.
    public record OutlineData(BlockPos pos, VoxelShape shape, Vec3 offset, Quaternionf rotation) {
        public OutlineData(BlockPos pos, VoxelShape shape) {
            this(pos, shape, Vec3.ZERO, null);
        }
    }

    public static void onOutlineExtract(ExtractBlockOutlineRenderStateEvent event) {
        var player = Minecraft.getInstance().player;
        var level = event.getLevel();
        var hitResult = event.getHitResult();

        if (player == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        var blockPos = hitResult.getBlockPos();
        var itemStack = player.getMainHandItem();

        var targetOutlines = new HashSet<OutlineData>();

        if (OritechClientConfig.showMachinePreview.get()) {
            addBlockPreviewOutlines(level, player, itemStack, blockPos, hitResult, targetOutlines);
            addParticlePlacementHelper(level, player, itemStack, blockPos, hitResult, targetOutlines);
        }

        addPromethiumPickaxeOutline(level, player, itemStack, blockPos, hitResult, targetOutlines);

        if (targetOutlines.isEmpty()) return;

        event.addCustomRenderer(((renderState, buffer, poseStack, translucentPass, levelRenderState) -> {
            var consumer = buffer.getBuffer(RenderTypes.lines());
            var cameraPos = levelRenderState.cameraRenderState.pos;

            for (var data : targetOutlines) {
                poseStack.pushPose();

                poseStack.translate(
                        data.pos().getX() - cameraPos.x,
                        data.pos().getY() - cameraPos.y,
                        data.pos().getZ() - cameraPos.z
                );

                if (data.offset() != null)
                    poseStack.translate(data.offset().x, data.offset().y, data.offset().z);

                if (data.rotation() != null)
                    poseStack.mulPose(data.rotation());

                ShapeRenderer.renderShape(poseStack, consumer, data.shape, 0, 0, 0, ColorHelper.argb(1f, 1f, 1f, 0.7f), 2f);

                poseStack.popPose();
            }

            return false; // this means vanilla outlines still render
        }));

    }

    private static void addBlockPreviewOutlines(ClientLevel level, LocalPlayer player, ItemStack itemStack, BlockPos blockPos, BlockHitResult hitResult, HashSet<OutlineData> targetOutlines) {

        var hasBlockItem = itemStack.getItem() instanceof BlockItem || itemStack.getItem().equals(ItemContent.UNSTABLE_CONTAINER.get());

        if (!hasBlockItem) return;

        var block = itemStack.getItem() instanceof BlockItem ? ((BlockItem) itemStack.getItem()).getBlock() : BlockContent.UNSTABLE_CONTAINER.get();

        if (!(block instanceof EntityBlock entityProvider) || !block.defaultBlockState().hasProperty(MultiblockMachine.ASSEMBLED))
            return;

        var machinePos = blockPos.offset(hitResult.getDirection().getUnitVec3i());
        if (itemStack.getItem().equals(ItemContent.UNSTABLE_CONTAINER.get()))
            machinePos = blockPos;

        var placementState = block.getStateForPlacement(new BlockPlaceContext(player, player.swingingArm, itemStack, hitResult));
        var entity = entityProvider.newBlockEntity(machinePos, placementState);

        if (!(entity instanceof MultiblockMachineController multiblockController)) return;

        if (itemStack.getItem().equals(ItemContent.UNSTABLE_CONTAINER.get())) {
            var blockState = level.getBlockState(machinePos);
            var isValid = BuiltInRegistries.BLOCK.wrapAsHolder(blockState.getBlock()).getData(DataMapContent.UNSTABLE_CONTAINER_SOURCE) != null;
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

        // build the shape in local space (relative to machinePos), the renderer translates it to the world position
        var shape = Shapes.empty();
        for (var coreOffset : fullList) {
            var localOffset = Geometry.rotatePosition(coreOffset, machineFacing);
            shape = Shapes.or(shape, Shapes.box(localOffset.getX(), localOffset.getY(), localOffset.getZ(), localOffset.getX() + 1, localOffset.getY() + 1, localOffset.getZ() + 1));
        }

        targetOutlines.add(new OutlineData(machinePos, shape));
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

    private static void addPromethiumPickaxeOutline(ClientLevel level, LocalPlayer player, ItemStack itemStack, BlockPos blockPos, BlockHitResult hitResult, HashSet<OutlineData> targetOutlines) {
        if (!itemStack.is(ToolsContent.PROMETHIUM_PICKAXE)) return;

        // returns in world space
        var offsetBlocks = PromethiumPickaxeItem.getOffsetBlocks(level, player, blockPos);

        for (var worldPos : offsetBlocks) {
            targetOutlines.add(new OutlineData(worldPos, level.getBlockState(worldPos).getShape(level, worldPos)));
        }

    }

    private static void addParticlePlacementHelper(ClientLevel level, LocalPlayer player, ItemStack itemStack, BlockPos blockPos, BlockHitResult hitResult, HashSet<OutlineData> targetOutlines) {

        var isRing = itemStack.is(BlockContent.ACCELERATOR_RING.asItem());
        var isMotor = itemStack.is(BlockContent.ACCELERATOR_MOTOR.asItem());

        if (!isRing && !isMotor) return;

        var facing = player.getDirection();
        var targetPos = blockPos.offset(hitResult.getDirection().getUnitVec3i());

        if (isMotor)
            facing = facing.getClockWise();

        var shape = Shapes.box(7 / 16f, 7 / 16f, 0, 9 / 16f, 9 / 16f, 1f);
        var halfShape = Shapes.box(4 / 16f, 7 / 16f, 0.8, 6 / 16f, 9 / 16f, 1.3f);
        var halfShapeLeft = Shapes.box(8 / 16f, 7 / 16f, 0.3, 10 / 16f, 9 / 16f, 0.8f);

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

        // slight offset to avoid z fighting, plus the per-facing offset that keeps the rotated shape inside the block
        var baseOffset = extraOffset.add(0.005f, 0.005f, 0.005f);

        targetOutlines.add(new OutlineData(targetPos, shape, baseOffset, Axis.YP.rotationDegrees(rotationY)));

        if (isRing) {
            targetOutlines.add(new OutlineData(targetPos, halfShape, baseOffset, Axis.YP.rotationDegrees(rotationY + 30)));
            targetOutlines.add(new OutlineData(targetPos, halfShapeLeft, baseOffset, Axis.YP.rotationDegrees(rotationY - 30)));
        }
    }
}
