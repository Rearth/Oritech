package rearth.oritech.api.screen.data;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import rearth.oritech.api.screen.UIComponent;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractDataDisplayWidget extends UIComponent {

    protected final long capacity;
    protected final Supplier<Long> amountSupplier;
    protected final Supplier<Component> tooltipSupplier;
    protected long currentAmount;
    protected double displayedAmount;

    protected AbstractDataDisplayWidget(DisplayDataSource source) {
        super(source.config().x(), source.config().y(), source.config().width(), source.config().height());
        this.capacity = source.capacity();
        this.amountSupplier = source.amountSupplier();
        this.tooltipSupplier = source.getTooltipSupplier();
        this.currentAmount = Mth.clamp(source.amountSupplier().get(), 0L, capacity);
        this.displayedAmount = currentAmount;
    }

    @Override
    public void tick() {
        // smoothly moves displayed amount to target amount
        currentAmount = getTargetAmount();
        
        if (!applySmoothing()) {
            displayedAmount = currentAmount;
        } else {
            displayedAmount += (currentAmount - displayedAmount) * 0.15f;
            
            if (Math.abs(currentAmount - displayedAmount) < 0.3f) {
                displayedAmount = currentAmount;
            }
        }
        
        
        this.setTooltip(List.of(tooltipSupplier.get()));
        
    }
    
    protected boolean applySmoothing() {
        return true;
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