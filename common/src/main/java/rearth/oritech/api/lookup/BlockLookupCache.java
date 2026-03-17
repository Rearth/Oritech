package rearth.oritech.api.lookup;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface BlockLookupCache<T> {
    static <T> BlockLookupCache<T> of(Supplier<@Nullable T> finder) {
        return finder::get;
    }

    @Nullable
    T find();

    default void invalidate() {
    }
}