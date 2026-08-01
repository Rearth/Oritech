package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;

import java.util.function.Supplier;

public final class SpaceAgeCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OritechSpaceAge.MOD_ID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemgroup.oritech_space_age.main"))
            .icon(() -> new ItemStack(SpaceAgeItems.ROCKET_ENGINE_TIER_1.get()))
            .displayItems((parameters, output) -> SpaceAgeItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private SpaceAgeCreativeTabs() {
    }
}
