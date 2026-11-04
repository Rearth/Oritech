package rearth.oritech.spaceage.client.hologram;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.OverlayWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.SurfaceWidget;
import rearth.oritech.client.ui.OritechWidgetScreen;
import rearth.oritech.spaceage.block.hologram.HoloControllerMenu;
import rearth.oritech.spaceage.block.hologram.HoloSettings;
import rearth.oritech.spaceage.block.hologram.HoloSnapshot;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.spaceage.network.HoloNetworking.Configure;
import rearth.oritech.spaceage.simulation.FleetCategories;
import rearth.oritech.spaceage.simulation.SpaceObjects;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HoloControllerScreen extends OritechWidgetScreen<HoloControllerMenu> {

    private HoloSettings settings;
    private HoloSnapshot received;
    private HoloPreview preview;
    private LabelWidget status;
    private ButtonWidget modeSelector;
    private ButtonWidget framingSelector;
    private boolean layoutDirty;
    private int page;
    private int ticks;
    private boolean dirty;
    private boolean choosingCraft;
    private String craftQuery = "";
    private EditBox craftSearch;
    private ScrollWidget craftChoices;
    private OverlayWidget craftPopup;

    public HoloControllerScreen(HoloControllerMenu menu, Inventory inventory, Component title) {

        super(menu, inventory, title, 590, 276);
    }

    static Component text(String key, Object... args) {

        return Component.translatable("screen.oritech_space_age.holo." + key, args);
    }

    @Override
    public BlockState getTitleState() {

        return SpaceAgeBlocks.HOLO_CONTROLLER.get().defaultBlockState();
    }

    @Override
    public boolean shouldCreateTitle() {

        return false;
    }

    private void send(boolean apply) {

        ClientPacketDistributor.sendToServer(new Configure(menu.pos, settings == null ? HoloSettings.DEFAULT : settings, apply));
        dirty = false;
    }

    private void change(String key, Object value) {

        var s = settings;
        settings = new HoloSettings(
                key.equals("mode") ? (String) value : s.mode(),
                key.equals("framing") ? (String) value : s.framing(),
                key.equals("category") ? (String) value : s.category(),
                key.equals("craft") ? (UUID) value : s.craft(),
                key.equals("centerX") ? (Double) value : s.centerX(),
                key.equals("centerY") ? (Double) value : s.centerY(),
                key.equals("coverage") ? (Double) value : s.coverage(),
                key.equals("minimum") ? (Double) value : s.minimum(),
                key.equals("maximum") ? (Double) value : s.maximum(),
                key.equals("margin") ? (Double) value : s.margin(),
                key.equals("height") ? (Double) value : s.height(),
                key.equals("rotation") ? (Double) value : s.rotation(),
                key.equals("markerSize") ? (Double) value : s.markerSize(),
                key.equals("layers") ? (Integer) value : s.layers(),
                key.equals("locked") ? (Boolean) value : s.locked(),
                key.equals("response") ? (Double) value : s.response()).normalized();
        dirty = true;
    }

    @Override
    protected void buildComponents() {

        layoutDirty = false;
        craftSearch = null;
        craftPopup = null;
        modeSelector = null;
        framingSelector = null;
        var snapshot = HoloRenderer.snapshot(menu.pos);
        if (settings == null) {
            settings = snapshot.settings();
            received = snapshot;
            send(false);
        }
        var panelWidth = Math.min(590, width - 12);
        setPanelSize(panelWidth, 276);
        addComponent(new SurfaceWidget(0, 0, panelWidth, 276, OritechSurface.PANEL));
        addComponent(new LabelWidget(10, 8, panelWidth - 20, title).withDarkColor());
        if (snapshot == HoloSnapshot.EMPTY) {
            addComponent(new LabelWidget(12, 35, panelWidth - 24, text("connecting")).withDarkColor());
            return;
        }
        var previewWidth = panelWidth - 244;
        preview = new HoloPreview(8, 30, previewWidth, 202, () -> HoloRenderer.snapshot(menu.pos), () -> settings,
                frame -> {
                    layoutDirty |= !settings.mode().equals("area");
                    change("centerX", frame.x());
                    change("centerY", frame.y());
                    change("coverage", frame.coverage());
                    change("mode", "area");
                    change("framing", "fixed");
                });
        addComponent(preview);
        var x = panelWidth - 228;
        addComponent(new LabelWidget(10, 239, previewWidth, 28, text("preview_help")).withDarkColor().withWrap(true));
        var tabs = List.of("display", "layers");
        for (int i = 0; i < tabs.size(); i++) {
            var tabIndex = i;
            var tab = SpaceAgeButtons.panel(x + i * 109, 30, 109, 20, text(tabs.get(i)), ignored -> {
                page = tabIndex;
                rebuildComponents();
            });
            if (page == i) {
                tab.withDisabledSurface(OritechSurface.PANEL_PRESSED).withDisabledTextColor(LabelWidget.BRIGHT_TEXT).withTextShadow(true);
                tab.setActive(false);
            }
            addComponent(tab);
        }
        if (page == 0) {
            modeSelector = valueButton(x, 55, "mode", text(settings.mode()), () -> {
                var modes = List.of("area", "category", "craft");
                var mode = modes.get((modes.indexOf(settings.mode()) + 1) % modes.size());
                change("mode", mode);
                change("framing", mode.equals("category") ? "fit" : mode.equals("craft") ? "follow" : "fixed");
                rebuildComponents();
            });
            var y = 79;
            if (settings.mode().equals("category")) {
                var categories = new FleetCategories(snapshot.categories());
                valueButton(x, y, "category", categories.label(settings.category()), () -> {
                    var all = categories.categories();
                    var current = 0;
                    for (int i = 0; i < all.size(); i++) if (all.get(i).id().equals(settings.category())) current = i;
                    change("category", all.get((current + 1) % all.size()).id());
                    rebuildComponents();
                });
                y += 24;
            } else if (settings.mode().equals("craft")) {
                var craft = snapshot.craft().stream().filter(c -> c.id().equals(settings.craft())).findFirst();
                valueButton(x, y, "craft", craft.<Component>map(c -> Component.literal(c.name())).orElse(text("none")), () -> {
                    choosingCraft = true;
                    craftQuery = "";
                    rebuildComponents();
                    setFocused(craftSearch);
                });
                y += 24;
            }
            framingSelector = valueButton(x, y, "framing", text(settings.framing()), () -> {
                var modes = List.of("fixed", "fit", "follow", "journey", "local");
                change("framing", modes.get((modes.indexOf(settings.framing()) + 1) % modes.size()));
                rebuildComponents();
            });
            button(x, y + 30, 105, "earth", () -> {
                change("mode", "area");
                change("framing", "fixed");
                change("centerX", (double) SpaceObjects.EARTH_X);
                change("centerY", 0d);
                change("coverage", 500_000d);
                rebuildComponents();
            });
            button(x + 109, y + 30, 109, "system", () -> {
                change("mode", "area");
                change("framing", "fixed");
                change("centerX", 0d);
                change("centerY", 0d);
                change("coverage", 20_000_000d);
                rebuildComponents();
            });
        } else {
            var names = List.of("bodies", "orbits", "paths", "surveys", "labels", "other_craft");
            for (int i = 0; i < names.size(); i++) {
                var mask = 1 << i;
                valueButton(x, 55 + i * 25, names.get(i), text(settings.shows(mask) ? "on" : "off"), () -> {
                    change("layers", settings.layers() ^ mask);
                    rebuildComponents();
                });
            }
            valueButton(x, 207, "lock", text(settings.locked() ? "on" : "off"), () -> {
                change("locked", !settings.locked());
                rebuildComponents();
            });
        }
        status = new LabelWidget(x, 241, 218, 27, text(snapshot.status(), snapshot.surface().width(), snapshot.surface().depth()))
                .withDarkColor().withWrap(true);
        addComponent(status);
        if (choosingCraft) buildCraftPopup(panelWidth);
    }

    private void button(int x, int y, int width, String key, Runnable action) {

        addComponent(SpaceAgeButtons.panel(x, y, width, 20, text(key), ignored -> action.run()));
    }

    private ButtonWidget valueButton(int x, int y, String key, Component value, Runnable action) {

        var button = SpaceAgeButtons.darkPanel(x, y, 218, 20, fit(text("setting", text(key), value), 204), ignored -> action.run());
        button.withTooltip(text(key + ".help"), value);
        addComponent(button);
        return button;
    }

    private Component fit(Component value, int width) {

        if (font.width(value) <= width) return value;
        return Component.literal(font.plainSubstrByWidth(value.getString(), width - font.width("…")) + "…");
    }

    private void closeCraftPopup() {

        choosingCraft = false;
        setFocused(null);
        rebuildComponents();
    }

    private void buildCraftPopup(int panelWidth) {

        var popupWidth = Math.min(330, panelWidth - 24);
        var px = (panelWidth - popupWidth) / 2;
        var py = 30;
        craftPopup = new OverlayWidget(panelWidth, 276).withDismissHandler(this::closeCraftPopup);
        craftPopup.addChild(new SurfaceWidget(px, py, popupWidth, 210, OritechSurface.PANEL_DARK));
        craftPopup.addChild(new LabelWidget(px + 10, py + 10, popupWidth - 42, text("choose_craft")).withBrightColor());
        craftPopup.addChild(SpaceAgeButtons.close(px + popupWidth - 26, py + 6, ignored -> closeCraftPopup()));
        craftChoices = new ScrollWidget(px + 8, py + 58, popupWidth - 16, 143).withVerticalScroll(true).withRenderCulling(2);
        craftPopup.addChild(craftChoices);
        addComponent(craftPopup);
        craftSearch = addWidget(new EditBox(font, leftPos + px + 10, topPos + py + 30, popupWidth - 20, 20, text("search_craft")));
        craftSearch.setMaxLength(64);
        craftSearch.setHint(text("search_craft"));
        craftSearch.setValue(craftQuery);
        craftSearch.setResponder(query -> {
            craftQuery = query;
            populateCraftChoices(popupWidth - 32);
        });
        populateCraftChoices(popupWidth - 32);
    }

    private void populateCraftChoices(int width) {

        craftChoices.getChildren().clear();
        var all = HoloRenderer.snapshot(menu.pos).craft().stream()
                .filter(craft -> craft.name().toLowerCase(Locale.ROOT).contains(craftQuery.strip().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(craft -> craft.name().toLowerCase(Locale.ROOT))).toList();
        var row = 0;
        for (var craft : all) {
            var choice = SpaceAgeButtons.panel(0, row, width - 8, 20, fit(Component.literal(craft.name()), width - 20), ignored -> {
                change("craft", craft.id());
                closeCraftPopup();
            });
            choice.withTooltip(Component.literal(craft.name()), Component.translatable(craft.status()));
            craftChoices.addChild(choice);
            row += 23;
        }
        if (all.isEmpty()) craftChoices.addChild(new LabelWidget(4, 6, width - 8, text("no_matches")).withDarkColor());
        craftChoices.setContentDimensions(width, Math.max(23, row));
        craftChoices.setScrollPosition(0, 0);
    }

    @Override
    protected void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        super.renderOverlays(graphics, mouseX, mouseY, partialTick);
        if (craftSearch != null) {
            graphics.nextStratum();
            craftSearch.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

        if (choosingCraft && craftPopup != null) return craftPopup.handleMouseScroll(mouseX - leftPos, mouseY - topPos, scrollY);
        var handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (layoutDirty) rebuildComponents();
        return handled;
    }

    @Override
    protected void containerTick() {

        super.containerTick();
        if (modeSelector != null) modeSelector.setLabel(fit(text("setting", text("mode"), text(settings.mode())), 204));
        if (framingSelector != null) framingSelector.setLabel(fit(text("setting", text("framing"), text(settings.framing())), 204));
        if (dirty && ++ticks % 5 == 0) send(true);
        var snapshot = HoloRenderer.snapshot(menu.pos);
        if (status != null) status.setText(text(snapshot.status(), snapshot.surface().width(), snapshot.surface().depth()));
        if (received == HoloSnapshot.EMPTY && snapshot != HoloSnapshot.EMPTY && !dirty) {
            settings = snapshot.settings();
            received = snapshot;
            rebuildComponents();
        }
    }

    @Override
    public void onClose() {

        if (dirty) send(true);
        super.onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        if (craftSearch != null && craftSearch.isMouseOver(event.x(), event.y())) {
            setFocused(craftSearch);
            return craftSearch.mouseClicked(event, doubleClick);
        }
        if (choosingCraft) return super.mouseClicked(event, doubleClick);
        setFocused(null);
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {

        var handled = super.mouseReleased(event);
        // Rebuild after the drag finishes so preview panning keeps its interaction target.
        if (layoutDirty) rebuildComponents();
        return handled;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        if (event.isEscape() && choosingCraft) {
            closeCraftPopup();
            return true;
        }
        if (!event.isEscape() && craftSearch != null && craftSearch.isFocused()) {
            getFocused().keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }
}
