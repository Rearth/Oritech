package rearth.oritech.block.logistics;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PipeCoreBlockItem extends BlockItem {

    public PipeCoreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    // this is used to create the first part of a pipe on a previously empty block
    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState placementState) {

        if (context.getLevel().isClientSide())
            return super.placeBlock(context, placementState);


        // create actual block + BE

        System.out.println("Before placing pipe at " + context.getClickedPos().toShortString());

        var success = super.placeBlock(context, placementState);

        System.out.println("After placing pipe, before setting start at " + context.getClickedPos().toShortString());

        if (!success) return false;

        // load and configure freshly created new block
        var level = context.getLevel();
        var pos = context.getClickedPos();

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeCoreBlockEntity pipe) {

            var position = getTargetPosition(context);
            pipe.setStartPart(position);

            pipe.updateConnections();


            System.out.println("After placing pipe, After setting start at " + context.getClickedPos().toShortString());

        }


        return true;
    }

    // if target position is an empty block, its usually that side. If its a non-full block, its usually the neighboring block
    private PipeCoreBlockEntity.PartPosition getTargetPosition(BlockPlaceContext context) {

        // direction in which the support / surface is. E.g. the block is on the opposite side of this.
        var supportDirection = context.getClickedFace().getOpposite();
        var side = context.getClickedFace();
        var level = context.getLevel();
        var pos = context.getClickedPos();

        var supportPos = pos.relative(supportDirection);
        var hasSolidSupport = level.getBlockState(supportPos).isFaceSturdy(level, supportPos, side);

        System.out.println("Placement data in item: " + pos.toShortString() + " | " + supportDirection + " | side: " + side + " solid: " + hasSolidSupport);

        if (!hasSolidSupport) return PipeCoreBlockEntity.PartPosition.CENTER;

        return PipeCoreBlockEntity.PartPosition.FromDirection(supportDirection);

    }
}
