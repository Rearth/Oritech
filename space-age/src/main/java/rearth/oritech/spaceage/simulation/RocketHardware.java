package rearth.oritech.spaceage.simulation;

import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import net.minecraft.world.level.block.Block;

/** Hardware is derived from the saved blocks, never supplied by a client. */
public record RocketHardware(int chemical, int ion, int scanners, int antennas) {
    public static RocketHardware of(StaticRocketSegment segment) {
        int chemical = 0, ion = 0;
        for (var block : segment.blocks()) {
            if (block.state().getBlock() instanceof RocketEngineBlock engine) {
                if (engine.getType() == RocketEngineBlock.Type.ION) ion++; else chemical++;
            }
        }
        return new RocketHardware(chemical, ion, count(segment, SpaceAgeBlocks.SPACE_SCANNER.get()),
                count(segment, SpaceAgeBlocks.ANTENNA.get()));
    }
    private static int count(StaticRocketSegment segment, Block block) {
        return (int) segment.blocks().stream().filter(b -> b.state().is(block)).count();
    }
    public double chemicalSeconds(DynamicRocketSegment resources) {
        return chemical == 0 ? 0 : resources.availableFuelBurnTimeTicks / (20.0 * chemical);
    }
    public double ionSeconds(DynamicRocketSegment resources) {
        return ion == 0 ? 0 : resources.availableRF / (20.0 * SpaceBalance.ION_RF * ion);
    }
    public boolean usesStationKeepingRF() {
        return antennas > 0 || ion > 0;
    }
    public boolean usesStationKeepingFuel() {
        return chemical > 0;
    }
    public void burn(DynamicRocketSegment resources, double seconds) {
        var previous = resources.availableFuelBurnTimeTicks;
        resources.availableFuelBurnTimeTicks = Math.max(0, previous - Math.round(seconds * 20 * chemical));
        if (resources.availableFuelBurnTimeTicks == 0) resources.currentFuelWeight = 0;
        resources.availableRF = Math.max(0, resources.availableRF - Math.round(seconds * 20 * SpaceBalance.ION_RF * ion));
    }
}
