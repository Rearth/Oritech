package rearth.oritech.spaceage.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.util.List;
import java.util.function.Supplier;

public final class SpaceAgeCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OritechSpaceAge.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemgroup.oritech_space_age.main"))
            .icon(() -> new ItemStack(SpaceAgeBlocks.BASIC_BOOSTER_ROCKET.get()))
            .displayItems((parameters, output) -> {
                SpaceAgeItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                var paintings = parameters.holders().lookupOrThrow(Registries.PAINTING_VARIANT);
                for (var name : List.of("earth", "mars", "sun")) {
                    var painting = new ItemStack(Items.PAINTING);
                    painting.set(DataComponents.PAINTING_VARIANT,
                            paintings.getOrThrow(ResourceKey.create(Registries.PAINTING_VARIANT, OritechSpaceAge.id(name))));
                    output.accept(painting);
                }
            })
            .build());

    private SpaceAgeCreativeTabs() {
    }
}
