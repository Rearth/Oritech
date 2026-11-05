package rearth.oritech.spaceage.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceAgeAdvancements;

import java.util.function.Consumer;

public final class SpaceAgeAdvancementProvider implements AdvancementSubProvider {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {

        Advancement.Builder.advancement()
                .display(SpaceAgeBlocks.SPACE_TROPHY.get(), Component.translatable("advancements.oritech_space_age.full_orbit"),
                        Component.translatable("advancements.oritech_space_age.full_orbit.description"),
                        Identifier.withDefaultNamespace("gui/advancements/backgrounds/end"), AdvancementType.CHALLENGE, true, true, false)
                .addCriterion(SpaceAgeAdvancements.CRITERION, new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance()))
                .save(output, SpaceAgeAdvancements.FULL_ORBIT.toString());
    }
}
