package rearth.oritech.spaceage.client.mission_control;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.mission_control.GroundStationBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

public final class MissionControlRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<GroundStationBlockEntity, R> {

    private static final DataTicket<Boolean> MISSION_CONTROL = DataTicket.create("mission_control_model", Boolean.class);

    private static final DataTicket<Integer> CONNECTIONS = DataTicket.create("mission_control_connections", Integer.class);

    public MissionControlRenderer(BlockEntityRendererProvider.Context context) {

        super(context, new Model());
    }

    @Override
    public void addRenderData(GroundStationBlockEntity entity, Void related, R state, float partialTick) {

        var block = entity.getBlockState();
        var missionControl = block.is(SpaceAgeBlocks.MISSION_CONTROL);
        state.addGeckolibData(MISSION_CONTROL, missionControl);
        var connections = 0;
        if (missionControl) {
            var facing = block.getValue(BlockStateProperties.HORIZONTAL_FACING);
            for (var direction : Direction.Plane.HORIZONTAL) {
                // Bone names are model-local; connection properties use world directions.
                var worldDirection = switch (facing) {
                    case EAST -> direction.getClockWise();
                    case SOUTH -> direction.getOpposite();
                    case WEST -> direction.getCounterClockWise();
                    default -> direction;
                };
                if (block.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(worldDirection))) connections |= 1 << direction.ordinal();
            }
        }
        state.addGeckolibData(CONNECTIONS, connections);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {

        var connections = pass.getGeckolibData(CONNECTIONS);
        for (var direction : Direction.Plane.HORIZONTAL) {
            var hidden = (connections & (1 << direction.ordinal())) == 0;
            bones.ifPresent("antenna_connection_" + direction.getSerializedName(), bone -> bone.skipRender(hidden));
        }
    }

    @Override
    public void submit(R state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {

        // Moving rocket and GUI previews can bypass shouldRender.
        if (Boolean.TRUE.equals(state.getGeckolibData(MISSION_CONTROL))) super.submit(state, poseStack, collector, camera);
    }

    @Override
    public boolean shouldRender(GroundStationBlockEntity entity, Vec3 cameraPosition) {

        // Scanners share the station entity type but keep their baked block model.
        return entity.getBlockState().is(SpaceAgeBlocks.MISSION_CONTROL) && super.shouldRender(entity, cameraPosition);
    }

    private static final class Model extends GeoModel<GroundStationBlockEntity> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {

            return OritechSpaceAge.id("mission_control");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {

            return OritechSpaceAge.id("textures/models/mission_control.png");
        }

        @Override
        public Identifier getAnimationResource(GroundStationBlockEntity entity) {

            return OritechSpaceAge.id("mission_control");
        }
    }
}
