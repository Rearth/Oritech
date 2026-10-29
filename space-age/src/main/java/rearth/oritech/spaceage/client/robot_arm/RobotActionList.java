package rearth.oritech.spaceage.client.robot_arm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.widgets.BlockWidget;
import rearth.oritech.api.screen.widgets.BoxWidget;
import rearth.oritech.api.screen.widgets.ButtonWidget;
import rearth.oritech.api.screen.widgets.ItemWidget;
import rearth.oritech.api.screen.widgets.LabelWidget;
import rearth.oritech.api.screen.widgets.ScrollWidget;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Action;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Kind;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Match;
import rearth.oritech.spaceage.client.SpaceAgeButtons;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Playbook rows shared by the arm and the card editor. Each row shows the recorded block (and the used item),
 * followed by the block match options.
 */
final class RobotActionList {

    static final int ROW_HEIGHT = 20;
    // Panel buttons draw a 2px bottom edge below their face, so they are taller than wide.
    private static final int BUTTON_WIDTH = 14;
    private static final int BUTTON_HEIGHT = 16;
    private static final int DIM_TEXT = ColorHelper.argb(0.42f, 0.42f, 0.45f);
    private static final int CURSOR_COLOR = ColorHelper.argb(1f, 0.6f, 0.2f, 0.35f);
    private static final int ROW_SEPARATOR = ColorHelper.argb(0f, 0f, 0f, 0.12f);
    private static final int SNEAK_COLOR = ColorHelper.argb(0.78f, 0.4f, 0.05f);
    // ScrollWidget keeps this margin around its viewport and draws the scrollbar inside it.
    private static final int SCROLL_MARGIN = 4;
    private static final int SCROLLBAR_GAP = 4;

    final ScrollWidget scroll;
    private final List<BoxWidget> highlights = new ArrayList<>();

    RobotActionList(int x, int y, int width, int height, RobotProgram program, Component emptyText,
                    BiConsumer<Integer, Match> onMatch, @Nullable IntConsumer onDelete) {

        // The rows sit directly on the parent panel, so the bounds given here are the visible rows.
        scroll = new ScrollWidget(x - SCROLL_MARGIN, y - SCROLL_MARGIN, width + SCROLL_MARGIN * 2, height + SCROLL_MARGIN * 2)
                .withRenderCulling(2);
        scroll.setSurface(OritechSurface.NONE);
        var rowWidth = width - SCROLLBAR_GAP;
        var actions = program.actions();
        for (int i = 0; i < actions.size(); i++) addRow(i, actions.get(i), rowWidth, onMatch, onDelete);
        if (actions.isEmpty())
            scroll.addChild(new LabelWidget(0, 0, rowWidth, height, emptyText).withColor(DIM_TEXT).withWrap(true));
        scroll.setContentDimensions(rowWidth, actions.size() * ROW_HEIGHT);
    }

    private static MutableComponent text(String key, Object... args) {

        return Component.translatable("screen.oritech_space_age.robot." + key, args);
    }

    /**
     * Sneaking changes what a click does, e.g. placing against a chest or shift-using a wrench.
     */
    private static UIComponent kindLabel(int x, int y, int width, Action action) {

        var kind = text(action.kind().name().toLowerCase(Locale.ROOT));
        var label = new LabelWidget(x, y, width, kind).withDarkColor();
        if (action.kind() == Kind.DESTROY) return label;
        if (action.sneaking()) label.setText(kind.copy().append(text("sneaking").withColor(SNEAK_COLOR)));
        return label.withTooltip(text(action.sneaking() ? "sneaking.tooltip" : "standing.tooltip"));
    }

    private static UIComponent blockIcon(int x, int y, BlockState state) {

        var tooltip = new ArrayList<Component>();
        tooltip.add(state.getBlock().getName());
        state.getValues().forEach(value -> tooltip.add(Component.literal(value.property().getName() + ": " + value.valueName())
                .withStyle(ChatFormatting.GRAY)));
        // Entity-rendered blocks such as chests have no block model to preview.
        UIComponent icon = state.getRenderShape() == RenderShape.MODEL
                ? new BlockWidget(x, y, 18, state)
                : new ItemWidget(x + 1, y + 1, new ItemStack(state.getBlock())).withShowOverlay(false).withTooltipFromStack(false);
        return icon.withTooltip(tooltip);
    }

    private static Component fit(String text, int width) {

        var font = Minecraft.getInstance().font;
        if (font.width(text) <= width) return Component.literal(text);
        return Component.literal(font.plainSubstrByWidth(text, width - font.width("…")) + "…");
    }

    void setCursor(int cursor) {

        for (int i = 0; i < highlights.size(); i++) highlights.get(i).setVisible(i == cursor);
    }

    void scrollTo(float scrollY) {

        scroll.setScrollPosition(0, scrollY);
    }

    private void addRow(int index, Action action, int width, BiConsumer<Integer, Match> onMatch, @Nullable IntConsumer onDelete) {

        var y = index * ROW_HEIGHT;
        var highlight = BoxWidget.filled(0, y, width, ROW_HEIGHT - 1, CURSOR_COLOR);
        highlight.setVisible(false);
        highlight.withZIndex(-1);
        highlights.add(highlight);
        scroll.addChild(highlight);
        scroll.addChild(BoxWidget.filled(0, y + ROW_HEIGHT - 1, width, 1, ROW_SEPARATOR));

        scroll.addChild(new LabelWidget(0, y + 5, 15, Component.literal(String.valueOf(index + 1)))
                .withColor(DIM_TEXT).withAlignment(LabelWidget.Alignment.RIGHT));
        scroll.addChild(blockIcon(19, y + 1, action.state()));
        var textX = 40;
        if (action.kind() == Kind.INTERACT && !action.item().isEmpty()) {
            scroll.addChild(new ItemWidget(39, y + 2, action.item()).withShowOverlay(false));
            textX = 58;
        }

        // Placements only need a replaceable target, so they have no match options.
        var matches = action.kind() == Kind.PLACE ? new Match[0] : Match.values();
        var deleteWidth = onDelete == null ? 0 : BUTTON_WIDTH + 3;
        var buttonsX = width - deleteWidth - matches.length * (BUTTON_WIDTH + 1) + 1;
        var textWidth = buttonsX - textX - 3;
        scroll.addChild(kindLabel(textX, y + 1, textWidth, action));
        scroll.addChild(new LabelWidget(textX, y + 10, textWidth, fit(action.pos().toShortString(), textWidth)).withColor(DIM_TEXT));

        for (var match : matches) {
            var button = new MatchButton(buttonsX + match.ordinal() * (BUTTON_WIDTH + 1), y + 2, match, match == action.match(),
                    ignored -> onMatch.accept(index, match));
            var key = "match." + match.name().toLowerCase(Locale.ROOT);
            button.withTooltip(text(key), text(key + ".check", action.state().getBlock().getName()).withStyle(ChatFormatting.GRAY));
            scroll.addChild(button);
        }
        if (onDelete != null) {
            var delete = SpaceAgeButtons.panel(width - BUTTON_WIDTH, y + 2, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("×"),
                    ignored -> onDelete.accept(index));
            delete.withTooltip(text("remove"));
            scroll.addChild(delete);
        }
    }

    /**
     * Draws the match option as a comparison glyph: a wildcard, equal to the block, or identical to the state.
     */
    private static final class MatchButton extends ButtonWidget {

        private final Match match;

        MatchButton(int x, int y, Match match, boolean selected, Consumer<ButtonWidget> onPress) {

            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.empty(), onPress);
            this.match = match;
            withDisabledSurface(OritechSurface.PANEL_PRESSED);
            setActive(!selected);
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {

            super.renderContent(graphics, mouseX, mouseY, delta);
            var left = getX() + 3;
            var top = getY() + 3 + (isActive() && isMouseOver(mouseX, mouseY) ? 1 : 0);
            var color = isActive() ? LabelWidget.DARK_TEXT : LabelWidget.BRIGHT_TEXT;
            switch (match) {
                case IGNORE -> {
                    graphics.fill(left + 3, top, left + 4, top + 7, color);
                    for (int i = 1; i <= 2; i++) {
                        graphics.fill(left + 3 - i, top + 3 - i, left + 4 - i, top + 4 - i, color);
                        graphics.fill(left + 3 + i, top + 3 - i, left + 4 + i, top + 4 - i, color);
                        graphics.fill(left + 3 - i, top + 3 + i, left + 4 - i, top + 4 + i, color);
                        graphics.fill(left + 3 + i, top + 3 + i, left + 4 + i, top + 4 + i, color);
                    }
                }
                case BLOCK -> {
                    graphics.fill(left, top + 1, left + 8, top + 3, color);
                    graphics.fill(left, top + 5, left + 8, top + 7, color);
                }
                case STATE -> {
                    for (int i = 0; i < 8; i += 3) graphics.fill(left, top + i, left + 8, top + i + 2, color);
                }
            }
        }
    }
}
