package rearth.oritech.spaceage.simulation.rocket;

import net.minecraft.world.level.block.Block;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.simulation.SpaceBalance;

/**
 * Hardware is derived from the saved blocks, never supplied by a client.
 */
public record RocketHardware(int chemical, int ion, int scanners, int antennas) {

    public static RocketHardware of(StaticRocketSegment segment) {

        var chemical = 0;
        var ion = 0;
        for (var block : segment.blocks()) {
            if (block.state().getBlock() instanceof RocketEngineBlock engine) {
                if (engine.getType() == RocketEngineBlock.Type.ION) ion++;
                else chemical++;
            }
        }
        return new RocketHardware(chemical, ion, count(segment, SpaceAgeBlocks.SPACE_SCANNER.get()),
                count(segment, SpaceAgeBlocks.ANTENNA.get()));
    }

    private static int count(StaticRocketSegment segment, Block block) {

        return (int) segment.blocks().stream().filter(b -> b.state().is(block)).count();
    }

    public double chemicalSeconds(DynamicRocketSegment resources) {

        return chemical == 0 ? 0 : resources.fuel() / fuelCost(1);
    }

    public double ionSeconds(DynamicRocketSegment resources) {

        return ion == 0 ? 0 : resources.rf() / rfCost(1);
    }

    public boolean usesStationKeepingFuel() {

        return chemical > 0;
    }

    public double fuelCost(double seconds) {

        return seconds * SpaceBalance.CHEMICAL_FUEL_PER_SECOND * chemical;
    }

    public double rfCost(double seconds) {

        return seconds * SpaceBalance.TICKS_PER_SECOND * SpaceBalance.ION_RF * ion;
    }

    public void burn(DynamicRocketSegment resources, double seconds) {

        resources.consumeFuel(fuelCost(seconds));
        resources.spendRF(rfCost(seconds));
    }
}
