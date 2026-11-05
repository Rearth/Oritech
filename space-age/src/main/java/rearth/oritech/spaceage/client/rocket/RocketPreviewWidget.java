package rearth.oritech.spaceage.client.rocket;

import net.minecraft.client.Minecraft;
import rearth.oritech.api.screen.widgets.BlockPreviewWidget;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.client.ui.render.PreviewEffect;
import rearth.oritech.spaceage.block.mission_control.MissionControlMenu;
import rearth.oritech.spaceage.client.rocket.RocketThrusters.Nozzle;

import java.util.List;

public final class RocketPreviewWidget extends BlockPreviewWidget {

    private final MissionControlMenu menu;
    private int revision = -1;
    private List<Nozzle> nozzles = List.of();

    public RocketPreviewWidget(int x, int y, int width, int height, MissionControlMenu menu) {

        super(x, y, width, height);
        this.menu = menu;
    }

    @Override
    protected float getEffectPadding() {

        return (float) nozzles.stream().mapToDouble(Nozzle::scale).max().orElse(1) * 4;
    }

    @Override
    protected List<PreviewEffect> getEffects(float partialTick) {

        var telemetry = menu.selectedTelemetry;
        var level = Minecraft.getInstance().level;
        if (telemetry == null || level == null || !OritechClientConfig.renderBeams()) return List.of();
        if (revision != menu.telemetryRevision) {
            revision = menu.telemetryRevision;
            nozzles = RocketThrusters.nozzles(telemetry.rocket());
        }
        var time = level.getGameTime() + partialTick;
        var elapsed = Math.clamp(time - menu.receivedWorldTick, 0, 40) * menu.debugSpeed;
        var entry = menu.fleet.stream().filter(candidate -> candidate.id().equals(menu.selected)).findFirst().orElse(null);
        if (entry == null) return List.of();
        var segments = RocketThrusters.firingSegments(telemetry, entry.navigation().paths(), elapsed);
        var correction = RocketThrusters.correctionBurn(telemetry, elapsed);
        return nozzles.stream().filter(nozzle -> nozzle.burnSeconds() > 0 && (correction || segments.contains(nozzle.segment())))
                .map(nozzle -> RocketThrusters.effect(nozzle, time)).toList();
    }
}
