package rearth.oritech.spaceage.client.beam_aperture;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.BlockPreviewWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ProgressBarWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.beam_aperture.BeamApertureBlockEntity;
import rearth.oritech.spaceage.block.beam_aperture.BeamApertureMenu;
import rearth.oritech.spaceage.block.beam_aperture.BeamApertureMenu.Snapshot;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.power.BeamAperture;
import rearth.oritech.util.TooltipHelper;

import java.util.List;
import java.util.Locale;

/** Compact, read-only ground receiver screen, following the machine screen layout. */
public final class BeamApertureScreen extends OritechWidgetScreen<BeamApertureMenu> {

    private BlockPreviewWidget preview;
    private LabelWidget status;
    private LabelWidget output;
    private LabelWidget coverage;
    private LabelWidget geometry;
    private LabelWidget heat;
    private ProgressBarWidget received;
    private ProgressBarWidget buffer;
    private Snapshot displayed;

    public BeamApertureScreen(BeamApertureMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 404, 196);
    }

    @Override
    public boolean shouldCreateTitle() {

        return false;
    }

    @Override
    protected void buildComponents() {

        var panelWidth = Math.min(imageWidth, width - 12);
        var previewWidth = Math.min(208, panelWidth - 196);
        var rightX = previewWidth + 24;
        var rightWidth = panelWidth - rightX - 12;
        setPanelSize(panelWidth, imageHeight);
        // Reserve the same separate title panel used by the machine screens.
        topPos = Math.max(30, topPos);
        addTitle(panelWidth);
        addComponent(new SurfaceWidget(0, 0, panelWidth, imageHeight, OritechSurface.PANEL));
        addComponent(new SurfaceWidget(8, 8, previewWidth, imageHeight - 16, OritechSurface.PANEL_INSET));
        preview = new BlockPreviewWidget(14, 14, previewWidth - 12, imageHeight - 28);
        addComponent(preview);
        status = new LabelWidget(rightX, 12, rightWidth - 22, 28, Component.empty()).withWrap(true);
        addComponent(status);
        var target = new ItemWidget(panelWidth - 30, 10, 18, new ItemStack(Items.COMPASS));
        target.withShowOverlay(false).withTooltipFromStack(false);
        target.withTooltip(text("target", menu.pos.getX(), menu.pos.getZ()));
        addComponent(target);
        received = new ProgressBarWidget(rightX, 43, rightWidth, 22, Component.empty(),
                () -> menu.snapshot.quality() > 0 ? (float) (menu.snapshot.received() / (menu.snapshot.quality() * BeamAperture.RF_PER_GLASS)) : 0f,
                () -> ProgressBarWidget.PRESET_GREEN).withTextColor(LabelWidget.DARK_TEXT);
        addComponent(received);
        coverage = new LabelWidget(rightX, 72, rightWidth, Component.empty()).withDarkColor();
        addComponent(coverage);
        buffer = new ProgressBarWidget(rightX, 94, rightWidth, 22, Component.empty(),
                () -> (float) menu.snapshot.energy() / BeamApertureBlockEntity.CAPACITY,
                () -> ProgressBarWidget.PRESET_BLUE).withTextColor(LabelWidget.DARK_TEXT);
        addComponent(buffer);
        output = new LabelWidget(rightX, 126, rightWidth, Component.empty()).withDarkColor();
        geometry = new LabelWidget(rightX, 146, rightWidth, Component.empty()).withDarkColor();
        heat = new LabelWidget(rightX, 166, rightWidth, Component.empty()).withDarkColor();
        addComponent(output);
        addComponent(geometry);
        addComponent(heat);
        displayed = null;
        updateDetails();
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        updateDetails();
    }

    private void updateDetails() {

        var data = menu.snapshot;
        if (data == displayed) return;
        if (displayed == null || !data.blocks().equals(displayed.blocks())) {
            preview.clearBlocks();
            for (var block : data.blocks()) preview.addBlock(block.state(), null, block.relativePos());
        }
        displayed = data;
        var issue = data.blocks().isEmpty() ? "loading" : !data.overworld() ? "space" : !data.upward() ? "facing" : data.defect().isPresent() ? "incomplete"
                : data.heat() >= 5 ? "hot" : data.energy() >= BeamApertureBlockEntity.CAPACITY ? "full"
                : data.received() > 0 ? "receiving" : "ready";
        status.setText(text("status." + issue));
        status.withColor(issue.equals("receiving") || issue.equals("ready") ? 0xFF398332 : 0xFFAA5724);
        status.setTooltip(data.defect().map(pos -> List.of(text("missing", pos.toShortString()))).orElse(List.of()));
        received.setTitle(text("received", energy(data.received())));
        received.withTooltip(text("capacity", energy(data.quality() * BeamAperture.RF_PER_GLASS)),
                text("rejected_capacity", energy(data.reception().capacityLoss())), text("rejected_storage", energy(data.reception().storageLoss())));
        coverage.setText(data.reception().incident() > 0 ? text("coverage", decimal(data.reception().coverage() * 100)) : text("coverage_idle"));
        coverage.withTooltip(text("incident", energy(data.reception().incident())), text("collected", energy(data.reception().collected())),
                text("missed", energy(data.reception().missed())), text("beam_radius", decimal(data.reception().radius())), text("collection_note"));
        buffer.setTitle(text("buffer", energy(data.energy())));
        buffer.withTooltip(text("stored", energy(data.energy()), energy(BeamApertureBlockEntity.CAPACITY)));
        output.setText(text("output", energy(data.exported())));
        output.withTooltip(text("exported", energy(data.exported()), energy(BeamApertureBlockEntity.MAX_OUTPUT)));
        geometry.setText(text("array", data.width(), data.height()));
        geometry.withTooltip(text("size", data.width(), data.height(), data.area()), text("quality", decimal(data.quality())), text("focus", Math.min(data.width(), data.height())));
        var heatState = text(data.heat() >= 10 ? "scorching" : data.heat() >= 5 ? "hot" : data.heat() >= 1 ? "warm" : "cool");
        heat.setText(text("heat", decimal(data.heat()), heatState));
        heat.withColor(data.heat() >= 5 ? 0xFFAA5724 : LabelWidget.DARK_TEXT);
    }

    private static String energy(double value) {

        return TooltipHelper.getEnergyText((long) value) + " RF";
    }

    private static String decimal(double value) {

        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static Component text(String key, Object... args) {

        return Component.translatable("screen.oritech_space_age.aperture." + key, args);
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.BEAM_APERTURE.get().defaultBlockState();
    }
}
