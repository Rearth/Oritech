package rearth.oritech.spaceage.network;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.*;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.init.*;
import rearth.oritech.spaceage.simulation.*;
import java.util.*;

public final class MissionNetworking {
    public record FleetEntry(UUID id, MissionState.Telemetry telemetry, boolean connected, boolean dismissible,
                             String route, SpaceCommunications.Connection communication) { }
    public record FleetPayload(List<FleetEntry> fleet, UUID system, List<SpaceSimulation.SpaceObjectData> objects, long simulationTick, int debugSpeed, SpaceCommunications.NetworkStatus network, List<SpaceCommunications.Node> nodes) implements CustomPacketPayload {
        public static final Type<FleetPayload> TYPE = new Type<>(OritechSpaceAge.id("fleet"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record FleetRequest(UUID selected, boolean edit) implements CustomPacketPayload {
        public static final Type<FleetRequest> TYPE = new Type<>(OritechSpaceAge.id("fleet_request"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record CardRequest(boolean save, SpaceSimulation.FlightPlan plan) implements CustomPacketPayload {
        public static final Type<CardRequest> TYPE = new Type<>(OritechSpaceAge.id("mission_card"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record SelectedPayload(MissionState.Position position, SpaceSimulation.FlightPlannerSnapshot snapshot, ActiveRocketData rocket, List<SpaceSimulation.FlightPlanAction> completed, String status, boolean connected) implements CustomPacketPayload {
        public static final Type<SelectedPayload> TYPE = new Type<>(OritechSpaceAge.id("selected_mission"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record DebugSpeed(int multiplier) implements CustomPacketPayload {
        public static final Type<DebugSpeed> TYPE = new Type<>(OritechSpaceAge.id("mission_debug_speed"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record DismissCraft(UUID id) implements CustomPacketPayload {
        public static final Type<DismissCraft> TYPE = new Type<>(OritechSpaceAge.id("dismiss_craft"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record SurveyResultsPayload(int discovered, int updated) implements CustomPacketPayload {
        public static final Type<SurveyResultsPayload> TYPE = new Type<>(OritechSpaceAge.id("survey_results"));
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    @SuppressWarnings("unchecked")
    public static void register(PayloadRegistrar registrar) {
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(MissionState.Position.CODEC), MissionState.Position.class);
        NetworkManager.registerCodec(ByteBufCodecs.fromCodecWithRegistries(MissionState.Telemetry.CODEC), MissionState.Telemetry.class);
        NetworkManager.getAutoCodec(SpaceCommunications.Connection.class);
        NetworkManager.getAutoCodec(SpaceCommunications.NetworkStatus.class);
        NetworkManager.getAutoCodec(SpaceCommunications.Node.class);
        NetworkManager.getAutoCodec(FleetEntry.class);
        registrar.playToClient(FleetPayload.TYPE, NetworkManager.getAutoCodec(FleetPayload.class), (payload, context) -> context.enqueueWork(() ->
                rearth.oritech.spaceage.client.MissionControlScreen.receive(payload)));
        registrar.playToClient(SelectedPayload.TYPE, NetworkManager.getAutoCodec(SelectedPayload.class), (payload, context) -> context.enqueueWork(() ->
                rearth.oritech.spaceage.client.MissionControlScreen.select(payload)));
        registrar.playToClient(SurveyResultsPayload.TYPE, NetworkManager.getAutoCodec(SurveyResultsPayload.class), (payload, context) -> context.enqueueWork(() ->
                rearth.oritech.spaceage.client.MissionNotifications.showSurveyResults(payload.discovered(), payload.updated())));
        registrar.playToServer(FleetRequest.TYPE, NetworkManager.getAutoCodec(FleetRequest.class), MissionNetworking::request);
        registrar.playToServer(DebugSpeed.TYPE, NetworkManager.getAutoCodec(DebugSpeed.class), (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isCreative()
                    || !(player.containerMenu instanceof MissionControlMenu menu) || !menu.stillValid(player)) return;
            if (payload.multiplier() != 1 && payload.multiplier() != 5 && payload.multiplier() != 10) return;
            MissionSavedData.get(player.level().getServer()).debugSpeed = payload.multiplier();
            sendFleet(player);
        });
        registrar.playToServer(DismissCraft.TYPE, NetworkManager.getAutoCodec(DismissCraft.class), (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof MissionControlMenu menu) || !menu.stillValid(player)) return;
            var data = MissionSavedData.get(player.level().getServer());
            if (!data.dismiss(player.getUUID(), payload.id())) return;
            data.setDirty();
            sendFleet(player);
        });
        registrar.playToServer(CardRequest.TYPE, NetworkManager.getAutoCodec(CardRequest.class), MissionNetworking::card);
    }
    public static void sendFleet(ServerPlayer player) {
        var data = MissionSavedData.get(player.level().getServer());
        var tick = MissionController.missionTime(player.level().getServer());
        var entries = data.craft.values().stream().filter(m -> m.owner.equals(player.getUUID())).map(mission ->
                new FleetEntry(mission.rocket.getRocketId(), mission.telemetry(tick), mission.canUpdateMission(), mission.canDismiss(),
                        mission.route, mission.communication)).toList();
        var nodes = data.networkNodes.stream().filter(n -> n.owner().equals(player.getUUID())).toList();
        PacketDistributor.sendToPlayer(player, new FleetPayload(entries, SpaceSimulationSavedData.getForPlayer(player).id(),
                SpaceSimulationSavedData.getForPlayer(player).createObjectData(), tick, data.debugSpeed,
                SpaceCommunications.network(player.getUUID(), nodes), nodes));
    }

    public static void sendSurveyResults(MinecraftServer server, UUID owner, int discovered, int updated) {
        var player = server.getPlayerList().getPlayer(owner);
        if (player != null && discovered + updated > 0)
            PacketDistributor.sendToPlayer(player, new SurveyResultsPayload(discovered, updated));
    }

    private static void request(FleetRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof MissionControlMenu menu) || !menu.stillValid(player)) return;
        sendFleet(player);
        if (!payload.edit) return;
        var data = MissionSavedData.get(player.level().getServer());
        var mission = data.craft.get(payload.selected);
        if (mission == null || !mission.owner.equals(player.getUUID())) return;
        boolean connected = mission.canUpdateMission();
        var known = mission.telemetry(MissionController.missionTime(player.level().getServer()));
        menu.selected = payload.selected;
        menu.setPreview(known.rocket());
        var system = SpaceSimulationSavedData.getForPlayer(player);
        PacketDistributor.sendToPlayer(player, new SelectedPayload(known.position(),
                new SpaceSimulation.FlightPlannerSnapshot(system.id(), known.rocket().getRocketId(), system.createObjectData(), known.plan()),
                known.rocket(), known.completed(), known.status(), connected));
    }

    public static void submit(ServerPlayer player, MissionControlMenu menu, SpaceSimulation.FlightPlan plan) {
        var mission = MissionSavedData.get(player.level().getServer()).craft.get(menu.selected);
        boolean accepted = mission != null && mission.owner.equals(player.getUUID())
                && MissionController.replace(mission, MissionController.resolveOrbitSlots(plan, MissionSavedData.get(player.level().getServer()), player.getUUID(), mission.rocket.getRocketId()), SpaceSimulationSavedData.getForPlayer(player));
        player.sendOverlayMessage(Component.translatable(accepted ? "message.oritech_space_age.mission_update_accepted"
                : "message.oritech_space_age.mission_update_rejected"));
        if (accepted) { mission.report(MissionController.missionTime(player.level().getServer()));
            var data = MissionSavedData.get(player.level().getServer()); data.receive(mission);
            MissionController.predictSeparations(data, mission, SpaceSimulationSavedData.getForPlayer(player).createObjectData(), MissionController.missionTime(player.level().getServer()));
            data.setDirty();
            sendFleet(player);
            var system = SpaceSimulationSavedData.getForPlayer(player);
            PacketDistributor.sendToPlayer(player, new SelectedPayload(mission.position,
                    new SpaceSimulation.FlightPlannerSnapshot(system.id(), mission.rocket.getRocketId(), system.createObjectData(), mission.plan),
                    mission.rocket, List.copyOf(mission.completed), mission.status, true));
        }
    }
    private static void card(CardRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof RocketAssemblerMenu menu) || !menu.stillValid(player)) return;
        var card = player.getMainHandItem().is(SpaceAgeItems.MISSION_CARD) ? player.getMainHandItem() : player.getOffhandItem();
        if (!card.is(SpaceAgeItems.MISSION_CARD)) { player.sendOverlayMessage(Component.translatable("message.oritech_space_age.hold_mission_card")); return; }
        var system = SpaceSimulationSavedData.getForPlayer(player);
        if (menu.getRocket() == null) return;
        if (payload.save) {
            var plan = validateCard(menu, payload.plan, system);
            if (plan == null || !sameActionCount(plan, payload.plan)) { player.sendOverlayMessage(Component.translatable("message.oritech_space_age.incompatible_mission")); return; }
            card.set(SpaceAgeComponents.MISSION.get(), new SpaceAgeComponents.MissionCard(system.id(), card.getHoverName().getString(), plan));
            player.sendOverlayMessage(Component.translatable("message.oritech_space_age.mission_saved"));
        } else {
            var stored = card.get(SpaceAgeComponents.MISSION.get());
            if (stored == null || !stored.system().equals(system.id())) { player.sendOverlayMessage(Component.translatable("message.oritech_space_age.invalid_mission_card")); return; }
            var plan = validateCard(menu, stored.plan(), system);
            if (plan == null || !sameActionCount(plan, stored.plan())) { player.sendOverlayMessage(Component.translatable("message.oritech_space_age.mission_card_mismatch")); return; }
            PacketDistributor.sendToPlayer(player, new RocketNetworking.FlightPlannerPayload(menu.blockPos,
                    new SpaceSimulation.FlightPlannerSnapshot(system.id(), menu.getRocket().getRocketId(), system.createObjectData(), plan)));
        }
    }
    private static SpaceSimulation.FlightPlan validateCard(RocketAssemblerMenu menu, SpaceSimulation.FlightPlan plan, SpaceSimulation system) {
        var anchors = menu.getRocket().getStaticSegments().values().stream().map(SpaceSimulation.SegmentRef::of).toList();
        if (plan.segmentConfigurations().stream().anyMatch(c -> !anchors.contains(c.segment()))) return null;
        return menu instanceof MissionControlMenu ? RocketFlightPlanRules.validateInFlight(plan, menu.getRocket(), system.createObjectData())
                : RocketFlightPlanRules.validate(plan, menu.getRocket(), system.createObjectData());
    }

    private static boolean sameActionCount(SpaceSimulation.FlightPlan a, SpaceSimulation.FlightPlan b) {
        return a.branches().stream().mapToInt(x -> x.actions().size()).sum() == b.branches().stream().mapToInt(x -> x.actions().size()).sum();
    }
    private MissionNetworking() { }
}
