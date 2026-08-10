package rearth.oritech.init.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import rearth.oritech.init.BlockContent;

import java.util.Arrays;
import java.util.List;

final class OritechJeiAliases {

    private static final String PREFIX = "alias.oritech.";

    private OritechJeiAliases() {}

    static void register(IIngredientAliasRegistration registration) {
        add(registration, "fluid_transport",
                BlockContent.FLUID_PIPE.get(), BlockContent.FRAMED_FLUID_PIPE.get(),
                BlockContent.FLUID_PIPE_DUCT.get(), BlockContent.FLUID_PIPE_CONNECTION.get(),
                BlockContent.FRAMED_FLUID_PIPE_CONNECTION.get());
        add(registration, "energy_transport",
                BlockContent.ENERGY_PIPE.get(), BlockContent.FRAMED_ENERGY_PIPE.get(),
                BlockContent.ENERGY_PIPE_DUCT.get(), BlockContent.ENERGY_PIPE_CONNECTION.get(),
                BlockContent.FRAMED_ENERGY_PIPE_CONNECTION.get(), BlockContent.SUPERCONDUCTOR.get(),
                BlockContent.FRAMED_SUPERCONDUCTOR.get(), BlockContent.SUPERCONDUCTOR_DUCT.get(),
                BlockContent.SUPERCONDUCTOR_CONNECTION.get(), BlockContent.FRAMED_SUPERCONDUCTOR_CONNECTION.get(),
                BlockContent.ENERGY_TRANSMISSION_POLE.get());
        add(registration, "item_transport",
                BlockContent.ITEM_PIPE.get(), BlockContent.TRANSPARENT_ITEM_PIPE.get(),
                BlockContent.FRAMED_ITEM_PIPE.get(), BlockContent.ITEM_PIPE_DUCT.get(),
                BlockContent.ITEM_PIPE_CONNECTION.get(), BlockContent.TRANSPARENT_ITEM_PIPE_CONNECTION.get(),
                BlockContent.FRAMED_ITEM_PIPE_CONNECTION.get(), BlockContent.ITEM_FILTER.get());
        add(registration, "pipe_extractor", BlockContent.PIPE_BOOSTER.get());

        add(registration, "crusher", BlockContent.PULVERIZER.get());
        add(registration, "fragment_forge", BlockContent.FRAGMENT_FORGE.get());
        add(registration, "auto_crafter", BlockContent.ASSEMBLER.get());
        add(registration, "alloy_smelter", BlockContent.FOUNDRY.get());
        add(registration, "freezer", BlockContent.INDUSTRIAL_CHILLER.get());
        add(registration, "separator", BlockContent.CENTRIFUGE.get());
        add(registration, "oil_processing", BlockContent.REFINERY.get(), BlockContent.TAINTED_REFINERY.get());
        add(registration, "electric_furnace", BlockContent.POWERED_FURNACE.get());
        add(registration, "ore_processing",
                BlockContent.PULVERIZER.get(), BlockContent.FRAGMENT_FORGE.get(), BlockContent.FOUNDRY.get(),
                BlockContent.INDUSTRIAL_CHILLER.get(), BlockContent.CENTRIFUGE.get(),
                BlockContent.ATOMIC_FORGE.get());

        add(registration, "power_generator",
                BlockContent.BIO_GENERATOR.get(), BlockContent.LAVA_GENERATOR.get(),
                BlockContent.FUEL_GENERATOR.get(), BlockContent.BASIC_GENERATOR.get(),
                BlockContent.STEAM_ENGINE.get(), BlockContent.BIG_SOLAR_PANEL.get(),
                BlockContent.NUCLEAR_REACTOR_CONTROLLER.get());
        add(registration, "battery",
                BlockContent.LARGE_STORAGE.get(), BlockContent.CREATIVE_STORAGE.get(),
                BlockContent.MACHINE_CAPACITOR_ADDON.get(), BlockContent.POWER_BANK_ADDON_EXTENDER.get());
        add(registration, "charger", BlockContent.EQUIPMENT_CHARGER.get());

        add(registration, "block_placer", BlockContent.PLACER.get());
        add(registration, List.of("quarry", "farmer"), BlockContent.DESTROYER.get());
        add(registration, "farmer", BlockContent.FERTILIZER.get(), BlockContent.TREE_CUTTER.get());
        add(registration, "fluid_pump", BlockContent.PUMP.get());
        add(registration, "wireless_transport", BlockContent.DRONE_PORT.get());
        add(registration, List.of("quarry", "combat"), BlockContent.ENDERIC_LASER.get());
        add(registration, "quarry", BlockContent.BEDROCK_EXTRACTOR.get());
        add(registration, "enchanting", BlockContent.STABILIZED_ENCHANTER.get());
        add(registration, "mob_spawner", BlockContent.SPAWNER_CONTROLLER.get(), BlockContent.SPAWNER_CAGE.get());
        add(registration, "particle_collider", BlockContent.PARTICLE_ACCELERATOR.get());
        add(registration, "cyberware",
                BlockContent.CYBERNETIC_AUGMENTATION_CENTER.get(),
                BlockContent.CYBERNETIC_RESEARCH_STATION.get(),
                BlockContent.QUANTUM_RESEARCH_STATION.get(), BlockContent.ARCANE_AUGMENT_STATION.get());

        var addons = new Block[]{
                BlockContent.MACHINE_SPEED_ADDON.get(), BlockContent.MACHINE_EFFICIENCY_ADDON.get(),
                BlockContent.SYNERGY_MATRIX_ADDON.get(), BlockContent.QUARRY_ADDON.get(),
                BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.get(), BlockContent.MACHINE_FLUID_ADDON.get(),
                BlockContent.MACHINE_YIELD_ADDON.get(), BlockContent.CROP_FILTER_ADDON.get(),
                BlockContent.MACHINE_HUNTER_ADDON.get(), BlockContent.MACHINE_CAPACITOR_ADDON.get(),
                BlockContent.MACHINE_ACCEPTOR_ADDON.get(), BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get(),
                BlockContent.MACHINE_EXTENDER.get(), BlockContent.POWER_BANK_ADDON_EXTENDER.get(),
                BlockContent.STEAM_BOILER_ADDON.get(), BlockContent.CONTROL_UNIT_ADDON.get(),
                BlockContent.MACHINE_SILK_TOUCH_ADDON.get(), BlockContent.MACHINE_BURST_ADDON.get(),
                BlockContent.HEART_OF_THE_MACHINE_ADDON.get()
        };
        add(registration, "upgrade", addons);
        add(registration, "overclock",
                BlockContent.MACHINE_SPEED_ADDON.get(), BlockContent.MACHINE_BURST_ADDON.get());
        add(registration, "parallel_processing", BlockContent.AUXILIARY_PROCESSING_CHAMBER_ADDON.get());
        add(registration, "fortune", BlockContent.MACHINE_YIELD_ADDON.get());
        add(registration, "farmer", BlockContent.CROP_FILTER_ADDON.get());
        add(registration, "combat", BlockContent.MACHINE_HUNTER_ADDON.get());
        add(registration, "battery",
                BlockContent.MACHINE_CAPACITOR_ADDON.get(), BlockContent.POWER_BANK_ADDON_EXTENDER.get());
        add(registration, "energy_input", BlockContent.MACHINE_ACCEPTOR_ADDON.get());
        add(registration, "inventory_access", BlockContent.MACHINE_INVENTORY_PROXY_ADDON.get());
        add(registration, "addon_expansion",
                BlockContent.MACHINE_EXTENDER.get(), BlockContent.POWER_BANK_ADDON_EXTENDER.get());
        add(registration, "steam_generator", BlockContent.STEAM_BOILER_ADDON.get());
        add(registration, "redstone_control", BlockContent.CONTROL_UNIT_ADDON.get());
        add(registration, "upgrade_combiner", BlockContent.ADDON_SPLICER.get());
    }

    private static void add(IIngredientAliasRegistration registration, String alias, Block... blocks) {
        add(registration, List.of(alias), blocks);
    }

    private static void add(IIngredientAliasRegistration registration, List<String> aliases, Block... blocks) {
        var stacks = Arrays.stream(blocks).map(ItemStack::new).toList();
        var translationKeys = aliases.stream().map(alias -> PREFIX + alias).toList();
        registration.addAliases(VanillaTypes.ITEM_STACK, stacks, translationKeys);
    }
}
