package rearth.oritech.block.logistics;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.Oritech;
import rearth.oritech.init.ItemContent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PipeToolPreview {

    private static final int MAX_BOXES = 1024;
    private static final Map<ServerPlayer, Integer> LAST_REQUEST_TICK = new WeakHashMap<>();

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Request.TYPE, Request.CODEC, PipeToolPreview::handleRequest);
        registrar.playToClient(Outline.TYPE, Outline.CODEC);
    }

    public record Request(int id, InteractionHand hand, BlockHitResult hit) implements CustomPacketPayload {
        private static final Type<Request> TYPE = new Type<>(Oritech.id("pipe_preview_request"));
        private static final StreamCodec<FriendlyByteBuf, BlockHitResult> HIT_CODEC =
            StreamCodec.of(FriendlyByteBuf::writeBlockHitResult, FriendlyByteBuf::readBlockHitResult);
        private static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Request::id,
            InteractionHand.STREAM_CODEC, Request::hand,
            HIT_CODEC, Request::hit,
            Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    public record Outline(int id, BlockPos origin, boolean reopen, List<AABB> boxes) implements CustomPacketPayload {
        public static final Type<Outline> TYPE = new Type<>(Oritech.id("pipe_preview_outline"));
        private static final StreamCodec<ByteBuf, AABB> BOX_CODEC = StreamCodec.of((buffer, box) -> {
            buffer.writeFloat((float) box.minX);
            buffer.writeFloat((float) box.minY);
            buffer.writeFloat((float) box.minZ);
            buffer.writeFloat((float) box.maxX);
            buffer.writeFloat((float) box.maxY);
            buffer.writeFloat((float) box.maxZ);
        }, buffer -> new AABB(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
            buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
        private static final StreamCodec<ByteBuf, Outline> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Outline::id,
            BlockPos.STREAM_CODEC, Outline::origin,
            ByteBufCodecs.BOOL, Outline::reopen,
            BOX_CODEC.apply(ByteBufCodecs.list(MAX_BOXES)), Outline::boxes,
            Outline::new);

        private Outline(int id, BlockPos origin, boolean reopen, VoxelShape shape) {
            this(id, origin, reopen, shape.toAabbs());
        }

        @Override
        public Type<Outline> type() {
            return TYPE;
        }
    }

    private static void handleRequest(Request request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;

        var level = player.level();
        var hit = request.hit();
        var pos = hit.getBlockPos();
        var offset = hit.getLocation().subtract(Vec3.atCenterOf(pos));
        if (!player.isWithinBlockInteractionRange(pos, 1) || hit.isWorldBorderHit()
            || !(Math.abs(offset.x) < 1.0000001 && Math.abs(offset.y) < 1.0000001 && Math.abs(offset.z) < 1.0000001)
            || PipeCoreBlock.getLoadedChunk(level, pos) == null || !level.mayInteract(player, pos)) return;

        var tick = level.getServer().getTickCount();
        var lastTick = LAST_REQUEST_TICK.put(player, tick);
        if (lastTick != null && lastTick == tick) return;

        var stack = player.getItemInHand(request.hand());
        var outline = new Outline(request.id(), pos, false, List.of());
        if (stack.getItem() instanceof PipeCoreBlockItem item
            && PipeCoreBlock.getLoadedChunk(level, pos.relative(hit.getDirection())) != null
            && PipeCoreBlock.getLoadedChunk(level, pos.relative(hit.getDirection().getOpposite())) != null) {
            var node = item.getPlacementTarget(new UseOnContext(player, request.hand(), hit));
            if (node != null) outline = new Outline(request.id(), node.pos(), false, PipeNetworks.previewPlacement(level, node));
        } else if (stack.is(ItemContent.WRENCH)) {
            var pipe = PipeCoreBlock.getLoadedPipe(level, pos);
            var target = pipe == null ? null : pipe.findWrenchTarget(hit);
            var networks = PipeNetworks.getExisting(level);
            if (target != null && networks != null) {
                var preview = networks.previewWrench(level, target.port());
                if (preview != null) outline = new Outline(request.id(), pos, preview.reopen(), preview.shape());
            }
        }

        if (outline.boxes().size() > MAX_BOXES) outline = new Outline(request.id(), pos, false, List.of());
        PacketDistributor.sendToPlayer(player, outline);
    }
}
