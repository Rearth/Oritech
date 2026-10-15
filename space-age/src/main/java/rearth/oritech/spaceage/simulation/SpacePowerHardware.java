package rearth.oritech.spaceage.simulation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public record SpacePowerHardware(double solar, double throughput, double focus, double area) {

    public static final Codec<SpacePowerHardware> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("solar").forGetter(SpacePowerHardware::solar), Codec.DOUBLE.fieldOf("throughput").forGetter(SpacePowerHardware::throughput),
            Codec.DOUBLE.fieldOf("focus").forGetter(SpacePowerHardware::focus), Codec.DOUBLE.fieldOf("area").forGetter(SpacePowerHardware::area)
    ).apply(i, SpacePowerHardware::new));

    public static SpacePowerHardware scan(List<StaticRocketSegment> segments) {

        var blocks = new HashMap<BlockPos, BlockState>();
        for (var segment : segments)
            for (var block : segment.blocks()) blocks.put(block.relativePos(), block.state());
        var claimed = new HashSet<BlockPos>();
        var throughput = 0d;
        var weightedFocus = 0d;
        var area = 0d;
        for (var pos : blocks.keySet().stream().sorted().toList()) {
            var state = blocks.get(pos);
            if (!state.is(SpaceAgeBlocks.BEAM_APERTURE)) continue;
            var array = BeamAperture.scan(pos, state.getValue(BlockStateProperties.FACING),
                    cell -> blocks.getOrDefault(cell, Blocks.AIR.defaultBlockState()));
            if (!array.valid() || array.cells().stream().anyMatch(claimed::contains)) continue;
            claimed.addAll(array.cells());
            throughput += array.throughput();
            area += array.area();
            weightedFocus += array.throughput() * array.focus();
        }
        return new SpacePowerHardware(SolarPower.rating(segments), throughput, throughput > 0 ? weightedFocus / throughput : 0, area);
    }
}
