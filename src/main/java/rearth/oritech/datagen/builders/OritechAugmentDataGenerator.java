package rearth.oritech.datagen.builders;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForgeMod;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.augmenter.api.CustomAugmentsCollection;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;
import rearth.oritech.init.datapack.AugmentData;
import rearth.oritech.init.datapack.AugmentContent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

import static rearth.oritech.util.TagUtils.cItemTag;

public class OritechAugmentDataGenerator implements DataProvider {

    private final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> registriesHolder;
    

    public OritechAugmentDataGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK,
                Registries.elementsDirPath(AugmentContent.AUGMENT_REGISTRY_KEY));
        registriesHolder = registries;
    }

    // no idea if this is the proper way of doing this
    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return this.registriesHolder.thenCompose(registries -> {
            var augmentData = new LinkedHashMap<Identifier, AugmentData>();
            addAugmentData(registries, (id, data) -> {
                var previous = augmentData.put(id, data);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate augment definition for id " + id);
                }
            });

            var registryOps = registries.createSerializationContext(JsonOps.INSTANCE);
            var tasks = new ArrayList<CompletableFuture<?>>();
            augmentData.forEach((id, data) -> {
                var encoded = AugmentData.CODEC.encodeStart(registryOps, data).getOrThrow(IllegalStateException::new);
                tasks.add(DataProvider.saveStable(output, encoded, pathProvider.json(id)));
            });

            return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Oritech augment data";
    }

    public void addAugmentData(HolderLookup.Provider registries, AugmentRecipeBuilder.Output exporter) {
        
        var CYBERNETIC_RESEARCH_STATION_ID = BuiltInRegistries.BLOCK.getKey(BlockContent.CYBERNETIC_RESEARCH_STATION.get());
        var QUANTUM_RESEARCH_STATION_ID = BuiltInRegistries.BLOCK.getKey(BlockContent.QUANTUM_RESEARCH_STATION.get());
        var ARCANE_AUGMENT_STATION_ID = BuiltInRegistries.BLOCK.getKey(BlockContent.ARCANE_AUGMENT_STATION.get());

        new AugmentRecipeBuilder(registries)
                .researchCost(TagContent.PLATING_BLOCKS, 64)
                .researchCost(TagContent.COAL_DUSTS, 32)
                .researchCost(ItemContent.BIOSTEEL_INGOT, 8)
                .applyCost(TagContent.PLATING_BLOCKS, 8)
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(5).uiY(70).time(400).rfCost(10_000_000)
                .modifierDefinition(Attributes.MAX_HEALTH, 6, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "hpboost");

        new AugmentRecipeBuilder(registries)
                .researchCost(TagContent.CARBON_FIBRE, 32)
                .researchCost(ItemContent.BIOSTEEL_INGOT, 16)
                .researchCost(Items.DIAMOND, 4)
                .applyCost(TagContent.CARBON_FIBRE, 8)
                .requirement(Oritech.id("augment/armor"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(80).uiY(70).time(800).rfCost(50_000_000)
                .modifierDefinition(Attributes.MAX_HEALTH, 4, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "hpboostmore");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENERGITE_INGOT, 64)
                .researchCost(ItemContent.REINFORCED_CARBON_SHEET, 32)
                .researchCost(Items.NETHER_STAR)
                .applyCost(ItemContent.ENERGITE_INGOT, 4)
                .requirement(Oritech.id("augment/ultimatearmor"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(165).uiY(70).time(1600).rfCost(200_000_000)
                .modifierDefinition(Attributes.MAX_HEALTH, 10, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "hpboostultra");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ADAMANT_INGOT, 32)
                .researchCost(Items.NETHER_STAR, 4)
                .researchCost(ItemContent.URANIUM_PELLET, 64)
                .researchCost(BlockContent.FLUXITE, 64)
                .applyCost(ItemContent.ADAMANT_INGOT, 4)
                .requirement(Oritech.id("augment/hpboostultra"))
                .requirement(Oritech.id("augment/gravity"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(205).uiY(40).time(2400).rfCost(500_000_000)
                .modifierDefinition(Attributes.MAX_HEALTH, 10, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "hpboostultimate");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MOTOR, 16)
                .researchCost(ItemContent.BIOSTEEL_INGOT, 32)
                .researchCost(Items.REDSTONE, 64)
                .applyCost(ItemContent.MOTOR, 4)
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(5).uiY(30).time(600).rfCost(30_000_000)
                .modifierDefinition(Attributes.MOVEMENT_SPEED, 0.25f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .export(exporter, "speedboost");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENERGITE_INGOT, 32)
                .researchCost(ItemContent.ION_THRUSTER, 16)
                .researchCost(ItemContent.FLUX_GATE, 16)
                .applyCost(ItemContent.ENERGITE_INGOT, 4)
                .requirement(Oritech.id("augment/speedboost"))
                .requirement(Oritech.id("augment/armor"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(55).uiY(50).time(1800).rfCost(150_000_000)
                .modifierDefinition(Attributes.MOVEMENT_SPEED, 0.25f, AttributeModifier.Operation.ADD_VALUE)
                .toggleable(true)
                .export(exporter, "superspeedboost");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MOTOR, 32)
                .researchCost(TagContent.STEEL_INGOTS, 64)
                .applyCost(ItemContent.MOTOR, 4)
                .requirement(Oritech.id("augment/superspeedboost"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(80).uiY(50).time(800).rfCost(75_000_000)
                .modifierDefinition(Attributes.STEP_HEIGHT, 0.6f, AttributeModifier.Operation.ADD_VALUE)
                .toggleable()
                .export(exporter, "stepassist");

        new AugmentRecipeBuilder(registries)
                .researchCost(Items.COPPER_INGOT, 64)
                .researchCost(ItemContent.PROCESSING_UNIT, 8)
                .researchCost(Items.GOLD_INGOT, 32)
                .applyCost(TagContent.SILICON, 4)
                .requirement(Oritech.id("augment/hpboost"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(30).uiY(90).time(400).rfCost(20_000_000)
                .modifierDefinition(Attributes.SCALE, -0.5f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .toggleable()
                .export(exporter, "dwarf");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.RAW_BIOPOLYMER, 32)
                .researchCost(TagContent.URANIUM_DUSTS, 4)
                .applyCost(ItemContent.RAW_BIOPOLYMER, 8)
                .requirement(Oritech.id("augment/dwarf"))
                .requirement(Oritech.id("augment/armor"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(55).uiY(90).time(1600).rfCost(40_000_000)
                .modifierDefinition(Attributes.SCALE, 1f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .toggleable()
                .export(exporter, "giant");

        new AugmentRecipeBuilder(registries)
                .researchCost(TagContent.STEEL_INGOTS, 64)
                .researchCost(ItemContent.DURATIUM_INGOT, 8)
                .researchCost(Items.DIAMOND, 16)
                .applyCost(ItemContent.DURATIUM_INGOT, 4)
                .applyCost(cItemTag("ingots/iron"), 32)
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(30).uiY(50).time(800).rfCost(80_000_000)
                .modifierDefinition(Attributes.ARMOR, 4, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "armor");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENERGITE_INGOT, 64)
                .researchCost(ItemContent.MAGNETIC_COIL, 32)
                .researchCost(Items.DIAMOND, 8)
                .applyCost(ItemContent.MAGNETIC_COIL, 4)
                .requirement(Oritech.id("augment/autofeeder"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(105).uiY(50).time(1600).rfCost(180_000_000)
                .modifierDefinition(Attributes.ARMOR, 6, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "betterarmor");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.FLUXITE, 64)
                .researchCost(ItemContent.HEISENBERG_COMPENSATOR, 32)
                .researchCost(ItemContent.PLUTONIUM_PELLET, 64)
                .researchCost(Items.NETHER_STAR, 4)
                .applyCost(ItemContent.HEISENBERG_COMPENSATOR, 1)
                .requirement(Oritech.id("augment/betterarmor"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(155).uiY(50).time(2400).rfCost(500_000_000)
                .modifierDefinition(Attributes.ARMOR, 8, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "ultimatearmor");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MAGNETIC_COIL, 32)
                .researchCost(TagContent.ELECTRUM_INGOTS, 48)
                .researchCost(Items.ENDER_PEARL, 4)
                .applyCost(ItemContent.MAGNETIC_COIL, 4)
                .requirement(Oritech.id("augment/blockreach"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(140).uiY(70).time(1600).rfCost(150_000_000)
                .modifierDefinition(Attributes.ENTITY_INTERACTION_RANGE, 0.3f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .export(exporter, "weaponreach");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MOTOR, 64)
                .researchCost(TagContent.STEEL_INGOTS, 48)
                .researchCost(Items.ENDER_PEARL, 4)
                .applyCost(ItemContent.MOTOR, 4)
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(115).uiY(90).time(900).rfCost(100_000_000)
                .modifierDefinition(Attributes.BLOCK_INTERACTION_RANGE, 0.3f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .export(exporter, "blockreach");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENDERIC_LENS, 64)
                .researchCost(Items.ENDER_PEARL, 16)
                .applyCost(ItemContent.ENDERIC_LENS, 4)
                .requirement(Oritech.id("augment/blockreach"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(140).uiY(90).time(800).rfCost(200_000_000)
                .modifierDefinition(Attributes.BLOCK_INTERACTION_RANGE, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .toggleable()
                .export(exporter, "farblockreach");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MAGNETIC_COIL, 48)
                .researchCost(Items.QUARTZ, 64)
                .researchCost(ItemContent.BASIC_BATTERY, 32)
                .applyCost(ItemContent.MAGNETIC_COIL, 4)
                .requirement(Oritech.id("augment/attackdamage"))
                .requirement(Oritech.id("augment/speedboost"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(30).uiY(10).time(1200).rfCost(50_000_000)
                .modifierDefinition(Attributes.BLOCK_BREAK_SPEED, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .export(exporter, "miningspeed");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENERGITE_INGOT, 64)
                .researchCost(ItemContent.FLUX_GATE, 48)
                .researchCost(ItemContent.DURATIUM_INGOT, 8)
                .applyCost(ItemContent.ENERGITE_INGOT, 4)
                .requirement(Oritech.id("augment/miningspeed"))
                .requirement(Oritech.id("augment/superspeedboost"))
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(80).uiY(10).time(2400).rfCost(250_000_000)
                .modifierDefinition(Attributes.BLOCK_BREAK_SPEED, 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .toggleable()
                .export(exporter, "superminingspeed");

        new AugmentRecipeBuilder(registries)
                .researchCost(TagContent.STEEL_INGOTS, 32)
                .researchCost(Items.DIAMOND, 8)
                .researchCost(ItemContent.FLUXITE, 64)
                .applyCost(TagContent.STEEL_INGOTS, 4)
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(5).uiY(10).time(1600).rfCost(150_000_000)
                .modifierDefinition(Attributes.ATTACK_DAMAGE, 4, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "attackdamage");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENDERIC_COMPOUND, 64)
                .researchCost(ItemContent.FLUXITE, 64)
                .applyCost(ItemContent.ENDERIC_COMPOUND, 4)
                .requirement(Oritech.id("augment/hpboostultra"))
                .requirement(Oritech.id("augment/ultimatearmor"))
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(180).uiY(50).time(2800).rfCost(500_000_000)
                .modifierDefinition(Attributes.ATTACK_DAMAGE, 6, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "superattackdamage");

        new AugmentRecipeBuilder(registries)
                .researchCost(TagContent.ELECTRUM_INGOTS, 64)
                .researchCost(cItemTag("storage_blocks/lapis"), 32)
                .researchCost(cItemTag("storage_blocks/gold"), 24)
                .applyCost(Items.LAPIS_LAZULI, 16)
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(55).uiY(30).time(1800).rfCost(200_000_000)
                .modifierDefinition(Attributes.LUCK, 5, AttributeModifier.Operation.ADD_VALUE)
                .export(exporter, "luck");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MAGNETIC_COIL, 64)
                .researchCost(ItemContent.FLUXITE, 48)
                .researchCost(Items.PHANTOM_MEMBRANE, 8)
                .applyCost(ItemContent.MAGNETIC_COIL, 4)
                .requirement(Oritech.id("augment/flight"))
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(180).uiY(10).time(2200).rfCost(300_000_000)
                .modifierDefinition(Attributes.GRAVITY, -0.5f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .toggleable()
                .export(exporter, "gravity");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ION_THRUSTER, 64)
                .researchCost(Items.WIND_CHARGE, 16)
                .researchCost(ItemContent.PROMETHEUM_INGOT, 16)
                .researchCost(ItemContent.PLUTONIUM_PELLET, 32)
                .applyCost(ItemContent.ION_THRUSTER, 4)
                .requirement(Oritech.id("augment/betterarmor"))
                .requirement(Oritech.id("augment/portal"))
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(155).uiY(30).time(3600).rfCost(500_000_000)
                .modifierDefinition(NeoForgeMod.CREATIVE_FLIGHT, 1, AttributeModifier.Operation.ADD_VALUE)
                .toggleable()
                .export(exporter, "flight");

        new AugmentRecipeBuilder(registries)
                .researchCost(Items.ENDER_EYE, 8)
                .researchCost(ItemContent.ENDERIC_LENS, 16)
                .researchCost(Items.DIAMOND, 8)
                .applyCost(ItemContent.ENDERIC_LENS, 4)
                .requirement(Oritech.id("augment/orefinder"))
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(155).uiY(10).time(3200).rfCost(100_000_000)
                .effectDefinition(MobEffects.INVISIBILITY, 0)
                .toggleable()
                .export(exporter, "cloak");

        new AugmentRecipeBuilder(registries)
                .researchCost(cItemTag("ender_pearls"), 16)
                .researchCost(Items.OBSIDIAN, 64)
                .researchCost(ItemContent.UNHOLY_INTELLIGENCE)
                .researchCost(ItemContent.ADAMANT_INGOT, 32)
                .applyCost(cItemTag("ender_pearls"), 4)
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(130).uiY(30).time(3000).rfCost(250_000_000)
                .customAugmentDefinition(CustomAugmentsCollection.portal.id)
                .toggleable()
                .export(exporter, "portal");

        new AugmentRecipeBuilder(registries)
                .researchCost(Items.GOLD_INGOT, 64)
                .researchCost(ItemContent.ENDERIC_LENS, 48)
                .researchCost(Items.GLOWSTONE_DUST, 64)
                .applyCost(Items.GLOWSTONE_DUST, 8)
                .requiredStation(QUANTUM_RESEARCH_STATION_ID)
                .uiX(105).uiY(30).time(2400).rfCost(50_000_000)
                .effectDefinition(MobEffects.NIGHT_VISION, 0)
                .toggleable()
                .export(exporter, "nightvision");

        new AugmentRecipeBuilder(registries)
                .researchCost(Items.PRISMARINE_CRYSTALS, 8)
                .researchCost(ItemContent.BIOSTEEL_INGOT, 32)
                .researchCost(Items.HEART_OF_THE_SEA)
                .applyCost(ItemContent.BIOSTEEL_INGOT, 4)
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(5).uiY(90).time(800).rfCost(50_000_000)
                .effectDefinition(MobEffects.WATER_BREATHING, 0)
                .export(exporter, "waterbreath");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.PROCESSING_UNIT, 16)
                .researchCost(TagContent.BIOMATTER, 64)
                .researchCost(Items.GOLDEN_CARROT, 64)
                .applyCost(TagContent.BIOMATTER, 4)
                .requirement(Oritech.id("augment/armor"))
                .requirement(Oritech.id("augment/hpboostmore"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(90).uiY(90).time(500).rfCost(30_000_000)
                .customAugmentDefinition(CustomAugmentsCollection.feeder.id)
                .toggleable()
                .export(exporter, "autofeeder");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.MAGNETIC_COIL, 32)
                .researchCost(ItemContent.ENERGITE_INGOT, 32)
                .researchCost(Items.LODESTONE, 2)
                .applyCost(ItemContent.MAGNETIC_COIL, 4)
                .requirement(Oritech.id("augment/superminingspeed"))
                .requiredStation(CYBERNETIC_RESEARCH_STATION_ID)
                .uiX(105).uiY(10).time(2400).rfCost(300_000_000)
                .customAugmentDefinition(CustomAugmentsCollection.magnet.id)
                .toggleable()
                .export(exporter, "magnet");

        new AugmentRecipeBuilder(registries)
                .researchCost(ItemContent.ENDERIC_LENS, 32)
                .researchCost(Items.SPYGLASS, 1)
                .researchCost(ItemContent.PROMETHEUM_INGOT, 1)
                .researchCost(Items.SCULK_SENSOR, 1)
                .applyCost(ItemContent.ENDERIC_LENS, 4)
                .requirement(Oritech.id("augment/nightvision"))
                .requirement(Oritech.id("augment/magnet"))
                .requiredStation(ARCANE_AUGMENT_STATION_ID)
                .uiX(130).uiY(10).time(3200).rfCost(200_000_000)
                .customAugmentDefinition(CustomAugmentsCollection.oreFinder.id)
                .toggleable()
                .export(exporter, "orefinder");
    }
}

