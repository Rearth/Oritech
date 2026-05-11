package rearth.oritech.api.screen.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.util.ColorHelper;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * Interactive slider for selecting an integer value inside an inclusive range.
 * Supports click-to-jump and click-drag updates.
 */
public class SliderWidget extends UIComponent {

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    public enum ValueLabelPosition {
        BEGIN,
        END
    }

    public static final int DISABLED_TEXT_COLOR = ColorHelper.argb(0.55f, 0.57f, 0.6f);
    public static final int KNOB_OUTLINE = ColorHelper.argb(0.12f, 0.12f, 0.13f);
    public static final int KNOB_COLOR = ColorHelper.argb(0.87f, 0.89f, 0.92f);
    public static final int KNOB_HOVER_COLOR = ColorHelper.argb(0.96f, 0.98f, 1f);
    public static final int KNOB_DISABLED_COLOR = ColorHelper.argb(0.6f, 0.62f, 0.66f);

    private static final int DEFAULT_CROSS_AXIS_SIZE = 18;
    private static final int BAR_HEIGHT = 6;
    private static final int KNOB_SIZE = 6;
    private static final int TITLE_BAR_GAP = 2;
    private static final int TITLE_VALUE_GAP = 4;

    private final BiConsumer<SliderWidget, Integer> onValueChanged;

    private Component title;
    private IntFunction<Component> valueFormatter = value -> Component.literal(Integer.toString(value));
    private int minValue;
    private int maxValue;
    private int value;
    private int textColor = LabelWidget.BRIGHT_TEXT;
    private int trackFillColor = ProgressBarWidget.PRESET_BLUE;
    private int length;
    private int crossAxisSize = DEFAULT_CROSS_AXIS_SIZE;
    private Orientation orientation = Orientation.HORIZONTAL;
    private ValueLabelPosition valueLabelPosition = ValueLabelPosition.END;
    private int[] snapValues;
    private boolean textShadow;
    private boolean active = true;
    private boolean dragging;
    private boolean logarithmicScale;

    public SliderWidget(int x, int y, int length, Component title, int minValue, int maxValue, int value,
                        BiConsumer<SliderWidget, Integer> onValueChanged) {
        this(x, y, length, Orientation.HORIZONTAL, ValueLabelPosition.END, title, minValue, maxValue, value, onValueChanged);
    }

    public SliderWidget(int x, int y, int width, int height, Component title, int minValue, int maxValue, int value,
                        BiConsumer<SliderWidget, Integer> onValueChanged) {
        super(x, y, width, height);
        this.title = title;
        this.onValueChanged = onValueChanged;
        this.orientation = height > width ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        this.length = this.orientation == Orientation.HORIZONTAL ? width : height;
        this.crossAxisSize = this.orientation == Orientation.HORIZONTAL ? height : width;
        setRange(minValue, maxValue);
        setValue(value);
    }

    public SliderWidget(int x, int y, int length, Orientation orientation, Component title, int minValue, int maxValue, int value,
                        BiConsumer<SliderWidget, Integer> onValueChanged) {
        this(x, y, length, orientation, ValueLabelPosition.END, title, minValue, maxValue, value, onValueChanged);
    }

    public SliderWidget(int x, int y, int length, Orientation orientation, ValueLabelPosition valueLabelPosition,
                        Component title, int minValue, int maxValue, int value,
                        BiConsumer<SliderWidget, Integer> onValueChanged) {
        super(x, y, 0, 0);
        this.title = title;
        this.onValueChanged = onValueChanged;
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;
        this.valueLabelPosition = valueLabelPosition != null ? valueLabelPosition : ValueLabelPosition.END;
        setLength(length);
        setRange(minValue, maxValue);
        setValue(value);
        
        
    }

    public Component getTitle() {
        return title;
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setRange(int minValue, int maxValue) {
        this.minValue = Math.min(minValue, maxValue);
        this.maxValue = Math.max(minValue, maxValue);
        this.value = clampValue(this.value);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = snapValue(clampValue(value));
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            dragging = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public SliderWidget withTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    public SliderWidget withTextShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public SliderWidget withTrackFillColor(int trackFillColor) {
        this.trackFillColor = trackFillColor;
        return this;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = Math.max(1, length);
        applyDimensions();
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;
        applyDimensions();
    }

    public SliderWidget withOrientation(Orientation orientation) {
        setOrientation(orientation);
        return this;
    }

    public SliderWidget withVertical(boolean vertical) {
        return withOrientation(vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL);
    }

    public ValueLabelPosition getValueLabelPosition() {
        return valueLabelPosition;
    }

    public void setValueLabelPosition(ValueLabelPosition valueLabelPosition) {
        this.valueLabelPosition = valueLabelPosition != null ? valueLabelPosition : ValueLabelPosition.END;
    }

    public SliderWidget withValueLabelPosition(ValueLabelPosition valueLabelPosition) {
        setValueLabelPosition(valueLabelPosition);
        return this;
    }

    public SliderWidget withValueFormatter(IntFunction<Component> valueFormatter) {
        if (valueFormatter != null) {
            this.valueFormatter = valueFormatter;
        }
        return this;
    }

    public SliderWidget withLogarithmicScale(boolean logarithmicScale) {
        this.logarithmicScale = logarithmicScale;
        this.value = snapValue(clampValue(this.value));
        return this;
    }

    public SliderWidget withSnapValues(int... snapValues) {
        if (snapValues == null || snapValues.length == 0) {
            this.snapValues = null;
            this.value = clampValue(this.value);
            return this;
        }

        this.snapValues = Arrays.stream(snapValues)
          .map(this::clampValue)
          .distinct()
          .sorted()
          .toArray();
        this.value = snapValue(this.value);
        return this;
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!active || button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        dragging = true;
        setValueFromMouse(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean handleDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (!active || !dragging || button != 0) {
            return false;
        }

        setValueFromMouse(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean handleMouseScroll(double mouseX, double mouseY, double scrollDelta) {
        if (!active || !isMouseOver(mouseX, mouseY) || scrollDelta == 0 || snapValues == null || snapValues.length == 0) {
            return false;
        }

        var direction = scrollDelta > 0 ? 1 : -1;
        var stepCount = Screen.hasShiftDown() ? 5 : 1;
        var nextValue = getSteppedSnapValue(direction * stepCount);
        if (nextValue != value) {
            value = nextValue;
            if (onValueChanged != null) {
                onValueChanged.accept(this, value);
            }
        }
        return true;
    }

    @Override
    public boolean handleMouseRelease(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;
        var cx = contentX();
        var cy = contentY();
        var cw = contentWidth();
        var ch = contentHeight();
        var valueLabel = getValueLabel();
        var headerHeight = getHeaderHeight(font, valueLabel);

        renderHeader(graphics, font, cx, cy, cw, valueLabel);

        var highlighted = active && (dragging || isMouseOver(mouseX, mouseY));
        if (orientation == Orientation.VERTICAL) {
            var barX = cx + Math.max(0, (cw - BAR_HEIGHT) / 2);
            var barY = cy + Math.min(ch, headerHeight);
            var barHeight = Math.max(0, ch - Math.min(ch, headerHeight));
            renderVerticalBar(graphics, barX, barY, BAR_HEIGHT, barHeight, highlighted);
        } else {
            var barY = cy + Math.min(ch - BAR_HEIGHT, headerHeight);
            renderHorizontalBar(graphics, cx, barY, cw, BAR_HEIGHT, highlighted);
        }
    }

    private void renderHorizontalBar(GuiGraphics graphics, int x, int y, int width, int height, boolean highlighted) {
        if (width <= 1 || height <= 1) {
            return;
        }

        graphics.fill(x, y, x + width, y + height,  ProgressBarWidget.BAR_OUTLINE);

        var innerX = x + 1;
        var innerY = y + 1;
        var innerWidth = width - 2;
        var innerHeight = height - 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        graphics.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, ProgressBarWidget.BAR_BACKGROUND);

        var progress = getProgress();
        if (progress > 0f) {
            var filledWidth = Mth.clamp(Math.round(innerWidth * progress), 1, innerWidth);
            graphics.fill(innerX, innerY, innerX + filledWidth, innerY + innerHeight, active ? trackFillColor : ProgressBarWidget.PRESET_GRAY);
        }

        drawHorizontalMarker(graphics, innerX, innerY + 1, innerWidth, Math.max(1, innerHeight - 2), 0.25f);
        drawHorizontalMarker(graphics, innerX, innerY + 1, innerWidth, Math.max(1, innerHeight - 2), 0.5f);
        drawHorizontalMarker(graphics, innerX, innerY + 1, innerWidth, Math.max(1, innerHeight - 2), 0.75f);

        var knobCenterX = innerX + Mth.clamp(Math.round((innerWidth - 1) * progress), 0, innerWidth - 1);
        var knobX = Mth.clamp(knobCenterX - KNOB_SIZE / 2, x, x + width - KNOB_SIZE);
        var knobY = y - 1;
        var knobHeight = height + 2;

        graphics.fill(knobX, knobY, knobX + KNOB_SIZE, knobY + knobHeight, KNOB_OUTLINE);
        graphics.fill(knobX + 1, knobY + 1, knobX + KNOB_SIZE - 1, knobY + knobHeight - 1,
            active ? (highlighted ? KNOB_HOVER_COLOR : KNOB_COLOR) : KNOB_DISABLED_COLOR);
    }

    private void renderVerticalBar(GuiGraphics graphics, int x, int y, int width, int height, boolean highlighted) {
        if (width <= 1 || height <= 1) {
            return;
        }

        graphics.fill(x, y, x + width, y + height, ProgressBarWidget.BAR_OUTLINE);

        var innerX = x + 1;
        var innerY = y + 1;
        var innerWidth = width - 2;
        var innerHeight = height - 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        graphics.fill(innerX, innerY, innerX + innerWidth, innerY + innerHeight, ProgressBarWidget.BAR_BACKGROUND);

        var progress = getProgress();
        if (progress > 0f) {
            var filledHeight = Mth.clamp(Math.round(innerHeight * progress), 1, innerHeight);
            var fillY = innerY + innerHeight - filledHeight;
            graphics.fill(innerX, fillY, innerX + innerWidth, innerY + innerHeight, active ? trackFillColor : ProgressBarWidget.PRESET_GRAY);
        }

        drawVerticalMarker(graphics, innerX + 1, innerY, Math.max(1, innerWidth - 2), innerHeight, 0.25f);
        drawVerticalMarker(graphics, innerX + 1, innerY, Math.max(1, innerWidth - 2), innerHeight, 0.5f);
        drawVerticalMarker(graphics, innerX + 1, innerY, Math.max(1, innerWidth - 2), innerHeight, 0.75f);

        var knobCenterY = innerY + innerHeight - 1 - Mth.clamp(Math.round((innerHeight - 1) * progress), 0, innerHeight - 1);
        var knobX = x - 1;
        var knobY = Mth.clamp(knobCenterY - KNOB_SIZE / 2, y, y + height - KNOB_SIZE);
        var knobWidth = width + 2;

        graphics.fill(knobX, knobY, knobX + knobWidth, knobY + KNOB_SIZE, KNOB_OUTLINE);
        graphics.fill(knobX + 1, knobY + 1, knobX + knobWidth - 1, knobY + KNOB_SIZE - 1,
            active ? (highlighted ? KNOB_HOVER_COLOR : KNOB_COLOR) : KNOB_DISABLED_COLOR);
    }

    private void drawHorizontalMarker(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        if (width <= 0 || height <= 0) {
            return;
        }

        var markerX = x + Mth.clamp(Math.round((width - 1) * progress), 0, width - 1);
        graphics.fill(markerX, y, markerX + 1, y + height, ProgressBarWidget.BAR_MARKER);
    }

    private void drawVerticalMarker(GuiGraphics graphics, int x, int y, int width, int height, float progress) {
        if (width <= 0 || height <= 0) {
            return;
        }

        var markerY = y + height - 1 - Mth.clamp(Math.round((height - 1) * progress), 0, height - 1);
        graphics.fill(x, markerY, x + width, markerY + 1, ProgressBarWidget.BAR_MARKER);
    }

    private Component getValueLabel() {
        return valueFormatter.apply(value);
    }

    private void renderHeader(GuiGraphics graphics, Font font, int x, int y, int width, Component valueLabel) {
        var color = active ? textColor : DISABLED_TEXT_COLOR;
        
        if (valueLabelPosition == ValueLabelPosition.BEGIN && valueLabel != null) {
            graphics.drawString(font, valueLabel, x, y, color, textShadow);
        }
        
        if (valueLabelPosition == ValueLabelPosition.END && valueLabel != null) {
            var valueX = orientation == Orientation.HORIZONTAL ? x + Math.max(0, width - font.width(valueLabel)) : x - font.width(valueLabel) / 2 + width / 2;
            var valueY = orientation == Orientation.VERTICAL ? y + height + TITLE_BAR_GAP : y;
            graphics.drawString(font, valueLabel, valueX, valueY, color, textShadow);
        }

        if (title != null) {
            var titleX = x;
            if (valueLabelPosition == ValueLabelPosition.BEGIN && valueLabel != null) {
                titleX += font.width(valueLabel) + TITLE_VALUE_GAP;
            }
            graphics.drawString(font, title, titleX, y, color, textShadow);
        }
    }

    private int getHeaderHeight(Font font, Component valueLabel) {
        return title != null || valueLabel != null ? font.lineHeight + TITLE_BAR_GAP : 0;
    }

    private float getProgress() {
        if (maxValue == minValue) {
            return 0f;
        }

        if (logarithmicScale) {
            var offsetValue = Math.max(1d, value - minValue + 1d);
            var offsetMax = Math.max(1d, maxValue - minValue + 1d);
            return (float) (Math.log(offsetValue) / Math.log(offsetMax));
        }

        return (float) (value - minValue) / (maxValue - minValue);
    }

    private void setValueFromMouse(double mouseX, double mouseY) {
        var progress = getMouseProgress(mouseX, mouseY);

        var nextValue = getValueForProgress(progress);
        if (nextValue != value) {
            value = nextValue;
            if (onValueChanged != null) {
                onValueChanged.accept(this, value);
            }
        }
    }

    private float getMouseProgress(double mouseX, double mouseY) {
        if (orientation == Orientation.VERTICAL) {
            var font = Minecraft.getInstance().font;
            var headerHeight = Math.min(height, getHeaderHeight(font, getValueLabel()));
            var innerY = y + headerHeight + 1;
            var innerHeight = Math.max(1, height - headerHeight - 2);
            var progress = (float) (1d - ((mouseY - innerY) / Math.max(1d, innerHeight - 1d)));
            return Mth.clamp(progress, 0f, 1f);
        }

        var innerX = x + 1;
        var innerWidth = Math.max(1, width - 2);
        var progress = (float) ((mouseX - innerX) / Math.max(1d, innerWidth - 1d));
        return Mth.clamp(progress, 0f, 1f);
    }

    private int clampValue(int value) {
        return Mth.clamp(value, minValue, maxValue);
    }

    private int snapValue(int rawValue) {
        if (snapValues == null || snapValues.length == 0) {
            return rawValue;
        }

        var clampedValue = clampValue(rawValue);
        var closestValue = snapValues[0];
        var closestDistance = Math.abs(closestValue - clampedValue);
        for (var candidate : snapValues) {
            var candidateDistance = Math.abs(candidate - clampedValue);
            if (candidateDistance < closestDistance) {
                closestValue = candidate;
                closestDistance = candidateDistance;
            }
        }
        return closestValue;
    }

    private int getSteppedSnapValue(int stepOffset) {
        if (snapValues == null || snapValues.length == 0) {
            return value;
        }

        var currentIndex = 0;
        for (var index = 0; index < snapValues.length; index++) {
            if (snapValues[index] >= value) {
                currentIndex = index;
                break;
            }
            currentIndex = index;
        }

        var targetIndex = Mth.clamp(currentIndex + stepOffset, 0, snapValues.length - 1);
        return snapValues[targetIndex];
    }

    private int getValueForProgress(float progress) {
        if (maxValue == minValue) {
            return minValue;
        }

        var nextValue = logarithmicScale
          ? minValue + (int) Math.round(Math.expm1(progress * Math.log(Math.max(1d, maxValue - minValue + 1d))))
          : minValue + Math.round(progress * (maxValue - minValue));
        return snapValue(clampValue(nextValue));
    }

    private void applyDimensions() {
        if (orientation == Orientation.VERTICAL) {
            super.setSize(crossAxisSize, length);
        } else {
            super.setSize(length, crossAxisSize);
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (orientation == Orientation.VERTICAL) {
            crossAxisSize = Math.max(1, width);
        } else {
            length = Math.max(1, width);
        }
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        if (orientation == Orientation.VERTICAL) {
            length = Math.max(1, height);
        } else {
            crossAxisSize = Math.max(1, height);
        }
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        if (orientation == Orientation.VERTICAL) {
            crossAxisSize = Math.max(1, width);
            length = Math.max(1, height);
        } else {
            length = Math.max(1, width);
            crossAxisSize = Math.max(1, height);
        }
    }
}