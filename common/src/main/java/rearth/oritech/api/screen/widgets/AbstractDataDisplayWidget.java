package rearth.oritech.api.screen.widgets;

import net.minecraft.util.Mth;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.data.DisplayDataSource;

import java.util.function.Supplier;

public abstract class AbstractDataDisplayWidget extends UIComponent {

    protected final long capacity;
    protected final Supplier<Long> amountSupplier;
    protected long currentAmount;
    protected double displayedAmount;

    protected AbstractDataDisplayWidget(int x, int y, int width, int height, DisplayDataSource dataSource) {
        super(x, y, width, height);
        this.capacity = dataSource.capacity();
        this.amountSupplier = dataSource.amountSupplier();
        this.currentAmount = Mth.clamp(dataSource.startAmount(), 0L, capacity);
        this.displayedAmount = currentAmount;
    }

    @Override
    public void tick() {
        currentAmount = getTargetAmount();
        displayedAmount += (currentAmount - displayedAmount) * 0.15f;

        if (Math.abs(currentAmount - displayedAmount) < 0.5f) {
            displayedAmount = currentAmount;
        }
    }

    protected long getCapacity() {
        return capacity;
    }

    protected long getCurrentAmount() {
        return currentAmount;
    }

    protected long getTargetAmount() {
        return Mth.clamp(amountSupplier.get(), 0L, capacity);
    }

    protected float getFillRatio() {
        if (capacity <= 0) {
            return 0f;
        }

        return Mth.clamp((float) (displayedAmount / capacity), 0f, 1f);
    }
}