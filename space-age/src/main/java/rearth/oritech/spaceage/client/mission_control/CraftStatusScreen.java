package rearth.oritech.spaceage.client.mission_control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.BlockPreviewWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.client.ui.render.BlockPreviewEntities;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.client.planner.RocketFlightPlannerScreen;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.MissionNetworking;
import rearth.oritech.spaceage.simulation.SpaceSimulation;
import rearth.oritech.spaceage.simulation.mission.MissionState;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Live craft dashboard sharing the mission menu and its unsent editor draft. */
public class CraftStatusScreen extends OritechWidgetScreen<MissionControlMenu> {

    private final Inventory inventory;
    private final Component screenTitle;
    private BlockPreviewWidget preview;
    private Map<BlockPos, BlockState> previewBlocks = Map.of();
    private int ticks;
    private int revision = -1;

    public CraftStatusScreen(MissionControlMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 0, 0);
        this.inventory = inventory;
        screenTitle = title;
    }

    @Override
    protected void buildComponents() {

        var view = preview == null ? null : preview.getViewState();
        var panelWidth = width - 12;
        var panelHeight = height - 12;
        setPanelSize(panelWidth, panelHeight);
        addComponent(new SurfaceWidget(0, 0, panelWidth, panelHeight, OritechSurface.PANEL));
        SpaceAgeButtons.missionTabs(1,
                () -> Minecraft.getInstance().setScreen(new MissionControlScreen(menu, inventory, screenTitle)),
                () -> {}, this::openEditor).forEach(this::addComponent);
        addCraftHeader(panelWidth);
        preview = new BlockPreviewWidget(9, 52, (panelWidth - 27) / 3, panelHeight - 61);
        preview.withSurface(OritechSurface.PANEL_INSET);
        preview.withRotationSpeed(.18f).withDragRotation().withViewState(view);
        previewBlocks = Map.of();
        updatePreview();
        addComponent(preview);
        addComponent(new CraftStatusWidget(preview.getX() + preview.getWidth() + 9, 52,
                panelWidth - preview.getWidth() - 27, panelHeight - 61, menu));
    }

    private void addCraftHeader(int panelWidth) {

        // Anchor to the outer panel, independently of the diagram's content bounds.
        var headerWidth = panelWidth - 264;
        addComponent(new UIComponent(255, 9, headerWidth, 34) {

            @Override
            protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

                var telemetry = menu.selectedTelemetry;
                if (telemetry == null) return;
                var lines = List.of(telemetry.plan().name().isBlank()
                                ? Component.translatable("screen.oritech_space_age.craft.title") : Component.literal(telemetry.plan().name()),
                        MissionState.statusComponent(telemetry.status()),
                        Component.translatable(menu.connected ? "screen.oritech_space_age.craft.connected"
                                : "screen.oritech_space_age.craft.disconnected"));
                setTooltip(lines);
                var colors = new int[]{0xFF404040, 0xFF606060, menu.connected ? 0xFF285522 : 0xFF806020};
                for (var row = 0; row < lines.size(); row++) {
                    var text = font.plainSubstrByWidth(lines.get(row).getString(), Math.max(0, getWidth()));
                    graphics.text(font, text, getX() + getWidth() - font.width(text), getY() + row * 12, colors[row], false);
                }
            }
        });
    }

    private void openEditor() {

        var telemetry = menu.selectedTelemetry;
        if (telemetry != null && menu.fleetSnapshot != null) {
            menu.setPreview(telemetry.rocket());
            menu.setFlightPlannerSnapshot(new SpaceSimulation.FlightPlannerSnapshot(menu.fleetSnapshot.simulationId(),
                    telemetry.rocket().getRocketId(), menu.fleetSnapshot.objects(), telemetry.plan(), menu.fleetSnapshot.stations()));
        }
        Minecraft.getInstance().setScreen(new RocketFlightPlannerScreen(menu, inventory, screenTitle));
    }

    private void updatePreview() {

        var telemetry = menu.selectedTelemetry;
        var rocket = telemetry != null && telemetry.fullLayout() ? telemetry.rocket() : menu.getRocket();
        if (rocket == null) return;
        var blocks = blocks(rocket);
        if (blocks.equals(previewBlocks)) return;
        previewBlocks = blocks;
        preview.clearBlocks();
        blocks.forEach((pos, state) -> preview.addBlock(state,
                BlockPreviewEntities.create(rocket.getLaunchPosition().offset(pos), state, minecraft.level), pos));
    }

    private static Map<BlockPos, BlockState> blocks(ActiveRocketData rocket) {

        var blocks = new HashMap<BlockPos, BlockState>();
        rocket.getStaticSegments().forEach((id, segment) -> {
            var resources = rocket.getDynamicSegments().get(id);
            segment.blocks().forEach(block -> blocks.put(block.relativePos(),
                    resources.reserved.getOrDefault(block.relativePos(), block.state())));
            segment.originalCouplings().values().forEach(couplings -> couplings.forEach(coupling ->
                    blocks.putIfAbsent(coupling.relativePos(), coupling.state())));
        });
        blocks.values().removeIf(BlockState::isAir);
        return blocks;
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (preview != null) preview.tick();
        if (++ticks >= 20) {
            ticks = 0;
            ClientPacketDistributor.sendToServer(MissionNetworking.FleetRequest.POLL);
        }
        if (menu.selected == null) {
            Minecraft.getInstance().setScreen(new MissionControlScreen(menu, inventory, screenTitle));
        } else if (revision != menu.telemetryRevision) {
            revision = menu.telemetryRevision;
            updatePreview();
        }
    }

    @Override
    public boolean shouldCreateTitle() {

        return false;
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.MISSION_CONTROL.get().defaultBlockState();
    }
}
