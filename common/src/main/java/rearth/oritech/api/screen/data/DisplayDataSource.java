package rearth.oritech.api.screen.data;

import java.util.function.Supplier;

public record DisplayDataSource(long capacity, long startAmount, Supplier<Long> amountSupplier) {
    
}
