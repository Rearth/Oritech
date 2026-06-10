package rearth.oritech.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rearth.oritech.Oritech;
import rearth.oritech.init.AttachmentContent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerZiplineHandler {

    private static final Map<UUID, Long> LAST_ZIPLINED_AT = new HashMap<>();

    public static void onZipLineTickUseEvent(ZiplinePlayerUsePacket packet, IPayloadContext context) {
        var player = context.player();
        LAST_ZIPLINED_AT.put(player.getUUID(), player.level().getGameTime());
    }

    // prevent fall damage / flying kick
    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) return;

        var id = player.getUUID();

        var startTime = LAST_ZIPLINED_AT.getOrDefault(id, 0L);
        var age = player.level().getGameTime() - startTime;

        if (age > 20) {
            LAST_ZIPLINED_AT.remove(id);
            if (player.getData(AttachmentContent.ZIPLINING_STATE))
                player.setData(AttachmentContent.ZIPLINING_STATE, false);
            return;
        }

        player.setData(AttachmentContent.ZIPLINING_STATE, true);
        player.resetFallDistance();
        if (player instanceof ServerPlayer serverPlayer)
            serverPlayer.connection.aboveGroundTickCount = 0;
    }

    public record ZiplinePlayerUsePacket() implements CustomPacketPayload {

        public static final Type<ZiplinePlayerUsePacket> PACKET_ID = new Type<>(Oritech.id("zipline_use"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }

}
