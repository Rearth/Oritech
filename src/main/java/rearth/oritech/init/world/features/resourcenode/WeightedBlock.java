package rearth.oritech.init.world.features.resourcenode;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

public record WeightedBlock(Identifier block, int weight, boolean required, List<Identifier> requiresAny) {
    public static final Codec<WeightedBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("block").forGetter(WeightedBlock::block),
        Codec.intRange(1, 1000)
            .optionalFieldOf("weight", 1).forGetter(WeightedBlock::weight),
        // Just like with a tag, any missing blocks with required: false will be ignored
        Codec.BOOL.optionalFieldOf("required", true).forGetter(WeightedBlock::required),
        // requiresAny is designed to prevent blocks from being placed without associated blocks
        // for example, an oritech:lead_resource_node should not be placed if there is no deepslate_lead_ore available
        Identifier.CODEC.listOf().optionalFieldOf("requiresAny", List.of())
            .forGetter(WeightedBlock::requiresAny)
    ).apply(instance, WeightedBlock::new));
}
