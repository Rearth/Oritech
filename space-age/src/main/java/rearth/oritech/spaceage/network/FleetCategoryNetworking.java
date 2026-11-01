package rearth.oritech.spaceage.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.client.mission_control.FleetViewPreferences;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;

import java.util.UUID;

/** Categories belong to the authenticated player's world simulation, independently of radio coverage. */
public final class FleetCategoryNetworking {

    private FleetCategoryNetworking() {
    }

    public static void register(PayloadRegistrar registrar) {

        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(FleetCategories.Saved.CODEC), FleetCategories.Saved.class);
        registrar.playToClient(CategoriesPayload.TYPE, NetworkManager.getAutoCodec(CategoriesPayload.class), (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof RocketAssemblerMenu)) return;
            var changed = FleetViewPreferences.receiveCategories(payload.system(), payload.categories());
            if (changed && context.player().containerMenu instanceof MissionControlMenu menu) menu.fleetRevision++;
        }));
        registrar.playToServer(UpdateCategories.TYPE, NetworkManager.getAutoCodec(UpdateCategories.class), (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && update(player, payload)) send(player);
        });
    }

    public static void send(ServerPlayer player) {

        var simulation = SpaceSimulationSavedData.getForPlayer(player);
        PacketDistributor.sendToPlayer(player, new CategoriesPayload(simulation.id(), simulation.fleetCategories.saved()));
    }

    public static boolean update(ServerPlayer player, UpdateCategories payload) {

        if (!(player.containerMenu instanceof RocketAssemblerMenu menu) || !menu.stillValid(player)) return false;
        var simulation = SpaceSimulationSavedData.getForPlayer(player);
        if (!simulation.id().equals(payload.system())) return false;
        simulation.fleetCategories.replace(payload.categories());
        SpaceSimulationSavedData.get(player.level().getServer()).setDirty();
        return true;
    }

    public record CategoriesPayload(UUID system, FleetCategories.Saved categories) implements CustomPacketPayload {

        public static final Type<CategoriesPayload> TYPE = new Type<>(OritechSpaceAge.id("fleet_categories"));

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }

    public record UpdateCategories(UUID system, FleetCategories.Saved categories) implements CustomPacketPayload {

        public static final Type<UpdateCategories> TYPE = new Type<>(OritechSpaceAge.id("update_fleet_categories"));

        @Override
        public Type<? extends CustomPacketPayload> type() {

            return TYPE;
        }
    }
}
