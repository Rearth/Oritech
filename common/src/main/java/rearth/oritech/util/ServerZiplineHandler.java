package rearth.oritech.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.attachment.AttachmentApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class ServerZiplineHandler {
    
    public static final Attachment<Boolean> ZIPLINING_STATE = new Attachment<>() {
        @Override
        public ResourceLocation identifier() {
            return Oritech.id("ziplining");
        }
        
        @Override
        public Codec<Boolean> persistenceCodec() {
            return Codec.BOOL;
        }
        
        @Override
        public StreamCodec<ByteBuf, Boolean> networkCodec() {
            return ByteBufCodecs.BOOL;
        }
        
        @Override
        public Supplier<Boolean> initializer() {
            return () -> false;
        }
    };
    
    private static final Map<UUID, Long> LAST_ZIPLINED_AT = new HashMap<>();
    
    public static void registerAttachments() {
        AttachmentApi.register(ZIPLINING_STATE);
    }
    
    public static void onZipLineTickUseEvent(ZiplinePlayerUsePacket packet, Player player, RegistryAccess dynamicRegistryManager) {
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
            if (AttachmentApi.getAttachmentValue(player, ZIPLINING_STATE))
                AttachmentApi.setAttachment(player, ZIPLINING_STATE, false);
            return;
        }
        
        AttachmentApi.setAttachment(player, ZIPLINING_STATE, true);
        player.resetFallDistance();
        if (player instanceof ServerPlayer serverPlayer)
            serverPlayer.connection.aboveGroundTickCount = 0;
    }
    
    public record ZiplinePlayerUsePacket() implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<ZiplinePlayerUsePacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("zipline_use"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
}
