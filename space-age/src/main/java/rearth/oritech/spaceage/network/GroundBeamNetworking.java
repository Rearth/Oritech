package rearth.oritech.spaceage.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.client.beam_aperture.GroundBeamRenderer;
import rearth.oritech.spaceage.simulation.power.SpacePowerService;
import rearth.oritech.spaceage.simulation.power.SpacePowerService.SurfaceBeam;

import java.util.List;

/** Physical beams are visible to nearby players independently of Mission Control and craft ownership. */
public final class GroundBeamNetworking {

    private GroundBeamNetworking() {}

    public static void register(PayloadRegistrar registrar) {

        registrar.playToClient(Beams.TYPE, NetworkManager.getAutoCodec(Beams.class), (payload, context) ->
                context.enqueueWork(() -> GroundBeamRenderer.receive(payload)));
    }

    public static void sync(MinecraftServer server, SpacePowerService power) {

        if (server.overworld().getGameTime() % 10 != 0) return;
        var targets = power.surfaceTargets(server);
        var range = server.getPlayerList().getViewDistance() * 16d;
        for (var player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            var nearby = targets.stream().filter(beam -> Math.hypot(beam.target().getX() + .5 - player.getX(), beam.target().getZ() + .5 - player.getZ()) <= range + beam.radius()).toList();
            PacketDistributor.sendToPlayer(player, new Beams(nearby));
        }
    }

    public record Beams(List<SurfaceBeam> targets) implements CustomPacketPayload {

        public static final Type<Beams> TYPE = new Type<>(OritechSpaceAge.id("ground_beams"));

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }
}
