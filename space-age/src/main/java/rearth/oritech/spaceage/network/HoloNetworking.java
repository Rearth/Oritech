package rearth.oritech.spaceage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.hologram.HoloControllerBlockEntity;
import rearth.oritech.spaceage.block.hologram.HoloControllerMenu;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.client.hologram.HoloRenderer;

public final class HoloNetworking {

    private HoloNetworking() {
    }

    public static void register(PayloadRegistrar registrar) {

        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(HoloSettings.CODEC), HoloSettings.class);
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(HoloSnapshot.CODEC), HoloSnapshot.class);
        registrar.playToClient(Projection.TYPE, NetworkManager.getAutoCodec(Projection.class), (p, c) -> c.enqueueWork(() -> HoloRenderer.receive(p)));
        registrar.playToServer(Configure.TYPE, NetworkManager.getAutoCodec(Configure.class), (p, c) -> {
            if (!(c.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof HoloControllerMenu menu)
                    || !menu.pos.equals(p.pos()) || !menu.stillValid(player)
                    || !(player.level().getBlockEntity(p.pos()) instanceof HoloControllerBlockEntity controller)) return;
            if (p.apply() && !configure(player, p)) player.sendOverlayMessage(Component.translatable("screen.oritech_space_age.holo.owner_only"));
            PacketDistributor.sendToPlayer(player, new Projection(p.pos(), controller.buildSnapshot()));
        });
    }

    public static boolean configure(ServerPlayer player, Configure payload) {

        if (!(player.containerMenu instanceof HoloControllerMenu menu) || !menu.pos.equals(payload.pos()) || !menu.stillValid(player)
                || !(player.level().getBlockEntity(payload.pos()) instanceof HoloControllerBlockEntity controller)) return false;
        var station = controller.station();
        if (station == null || !station.owner.equals(player.getUUID()) && !player.isCreative()) return false;
        controller.settings = payload.settings().normalized();
        controller.setChanged();
        return true;
    }

    public record Projection(BlockPos pos, HoloSnapshot snapshot) implements CustomPacketPayload {

        public static final Type<Projection> TYPE = new Type<>(OritechSpaceAge.id("holo_projection"));

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }

    public record Configure(BlockPos pos, HoloSettings settings, boolean apply) implements CustomPacketPayload {

        public static final Type<Configure> TYPE = new Type<>(OritechSpaceAge.id("holo_configure"));

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }
}
