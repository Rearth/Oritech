package rearth.oritech.spaceage.simulation;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Shared, deliberately small balancing model for planner collision and landing predictions.
 */
public final class AsteroidImpactRules {

    public static final double KILOGRAMS_PER_ASTEROID_MASS = 1_000;
    public static final double SPACE_DRAG_PER_SECOND = 0.000002;
    public static final double MAX_ASTEROID_CONNECTION_SPEED = 12;
    private static final double SAFE_ASTEROID_SPEED = 6;
    private static final double ASTEROID_CHIP_ENERGY_PER_KILOGRAM = 30_000;
    private static final double ASTEROID_BREAKUP_ENERGY_PER_KILOGRAM = 180_000;
    private static final double HEAVY_IMPACT_ENERGY = 100_000_000;

    private AsteroidImpactRules() {
    }

    public static int landingUncertaintyBlocks(double distanceFromSurface) {

        return Mth.clamp(32 + (int) Math.ceil(Math.max(0, distanceFromSurface) / 1_000),
                32, 100_000);
    }

    public static boolean canConnectAsteroid(double relativeSpeed) {

        return relativeSpeed <= MAX_ASTEROID_CONNECTION_SPEED;
    }

    public static ImpactPrediction predictArrival(double rocketMass, SpaceSimulation.SpaceObjectData target,
                                                  double relativeSpeed, SpaceSimulation.SpaceObjectData attached,
                                                  SpaceSimulation.FlightPlanAction action) {

        return predictArrival(rocketMass, target, relativeSpeed, attached, action, 0, false);
    }

    public static ImpactPrediction predictArrival(double rocketMass, SpaceSimulation.SpaceObjectData target,
                                                  double relativeSpeed, SpaceSimulation.SpaceObjectData attached,
                                                  SpaceSimulation.FlightPlanAction action, double explosiveEnergy, boolean detonation) {

        var asteroidMass = attached == null ? 0 : attached.mass() * KILOGRAMS_PER_ASTEROID_MASS;
        var combinedMass = Math.max(1, rocketMass + asteroidMass);
        var kinetic = combinedMass * relativeSpeed * relativeSpeed * 0.5;
        var safeSpeed = attached == null ? MAX_ASTEROID_CONNECTION_SPEED : SAFE_ASTEROID_SPEED;
        var explosive = detonation || relativeSpeed > safeSpeed ? Math.max(0, explosiveEnergy) : 0;
        var energy = kinetic + explosive;
        var landingX = action.landingX() + action.landingOffsetX();
        var landingZ = action.landingZ() + action.landingOffsetZ();

        if (target.type() == SpaceObjects.ObjectType.ASTEROID) {
            var targetMass = Math.max(1, target.mass() * KILOGRAMS_PER_ASTEROID_MASS);
            var energyPerKilogram = energy / targetMass;
            if (energyPerKilogram >= ASTEROID_CHIP_ENERGY_PER_KILOGRAM) {
                var mode = energyPerKilogram >= ASTEROID_BREAKUP_ENERGY_PER_KILOGRAM
                        ? FragmentationMode.CATASTROPHIC : FragmentationMode.CHIPPING;
                var fragments = createFragments(target, energy, energyPerKilogram, mode);
                var fragmentedMass = fragments.stream().mapToDouble(AsteroidFragment::mass).sum();
                var remainingMass = mode == FragmentationMode.CATASTROPHIC ? 0
                        : (float) Math.max(0, target.mass() - fragmentedMass);
                return new ImpactPrediction(ArrivalOutcome.ASTEROID_FRAGMENTATION, relativeSpeed, kinetic, 0,
                        mode, fragments, remainingMass, List.of(), landingX, landingZ, explosive);
            }
            return new ImpactPrediction(canConnectAsteroid(relativeSpeed) && !detonation
                    ? ArrivalOutcome.SAFE_APPROACH : ArrivalOutcome.ROCKET_DESTRUCTION,
                    relativeSpeed, kinetic, 0, FragmentationMode.NONE, List.of(), target.mass(),
                    List.of(), landingX, landingZ, explosive);
        }

        ArrivalOutcome outcome = relativeSpeed <= safeSpeed && !detonation ? ArrivalOutcome.SAFE_APPROACH
                : energy >= HEAVY_IMPACT_ENERGY || attached != null
                  ? ArrivalOutcome.HEAVY_IMPACT : ArrivalOutcome.ROCKET_DESTRUCTION;
        var craterRadius = outcome == ArrivalOutcome.HEAVY_IMPACT
                ? Mth.clamp((int) Math.round(3 * Math.cbrt(energy / HEAVY_IMPACT_ENERGY)), 3, 48) : 0;
        var recovery = relativeSpeed <= safeSpeed ? 1 : Mth.clamp(0.85 - relativeSpeed / 500, 0.15, 0.8);
        var recovered = attached == null ? List.<SpaceObjects.AsteroidMaterial>of()
                : scaleMaterials(attached.materials(), recovery);
        return new ImpactPrediction(outcome, relativeSpeed, kinetic, craterRadius, FragmentationMode.NONE,
                List.of(), 0, recovered, landingX, landingZ, explosive);
    }

    public static List<SpaceObjects.AsteroidMaterial> scaleMaterials(
            List<SpaceObjects.AsteroidMaterial> materials, double fraction) {

        return materials.stream().map(material -> new SpaceObjects.AsteroidMaterial(
                        material.block(), Math.max(0, (int) Math.floor(material.amount() * fraction))))
                .filter(material -> material.amount() > 0).toList();
    }

    private static List<AsteroidFragment> createFragments(SpaceSimulation.SpaceObjectData target, double energy,
                                                          double energyPerKilogram, FragmentationMode mode) {

        var count = mode == FragmentationMode.CHIPPING
                ? Mth.clamp(2 + (int) (energyPerKilogram / 75_000), 2, 4)
                : Mth.clamp(4 + (int) (energyPerKilogram / 250_000), 4, 8);
        var fragmentedFraction = mode == FragmentationMode.CATASTROPHIC ? 1
                : Mth.clamp(0.04 + (energyPerKilogram - ASTEROID_CHIP_ENERGY_PER_KILOGRAM)
                                   / (ASTEROID_BREAKUP_ENERGY_PER_KILOGRAM - ASTEROID_CHIP_ENERGY_PER_KILOGRAM) * 0.16,
                0.04, 0.2);
        var random = new Random(target.id().getMostSignificantBits() ^ target.id().getLeastSignificantBits()
                ^ Double.doubleToLongBits(energy));
        var weights = new double[count];
        double totalWeight = 0;
        for (int index = 0; index < count; index++) {
            weights[index] = 0.04 + Math.pow(random.nextDouble(), mode == FragmentationMode.CHIPPING ? 3 : 2);
            totalWeight += weights[index];
        }
        var fractions = new double[count];
        for (int index = 0; index < count; index++)
            fractions[index] = fragmentedFraction * weights[index] / totalWeight;

        var materials = distributeMaterials(target.materials(), fractions);
        var fragments = new ArrayList<AsteroidFragment>();
        for (int index = 0; index < count; index++) {
            var mass = (float) (target.mass() * fractions[index]);
            var radius = (float) (target.radius() * Math.cbrt(fractions[index]));
            fragments.add(new AsteroidFragment(mass, radius, materials.get(index)));
        }
        fragments.sort(Comparator.comparingDouble(AsteroidFragment::mass).reversed());
        return List.copyOf(fragments);
    }

    private static List<List<SpaceObjects.AsteroidMaterial>> distributeMaterials(
            List<SpaceObjects.AsteroidMaterial> source, double[] fractions) {

        var result = new ArrayList<List<SpaceObjects.AsteroidMaterial>>();
        for (int index = 0; index < fractions.length; index++) result.add(new ArrayList<>());
        var fragmentedFraction = Arrays.stream(fractions).sum();
        for (var material : source) {
            var fragmentedAmount = (int) Math.round(material.amount() * fragmentedFraction);
            var amounts = new int[fractions.length];
            var remainders = new double[fractions.length];
            var assigned = 0;
            for (int index = 0; index < fractions.length; index++) {
                var exact = fragmentedAmount * fractions[index] / fragmentedFraction;
                amounts[index] = (int) Math.floor(exact);
                remainders[index] = exact - amounts[index];
                assigned += amounts[index];
            }
            while (assigned < fragmentedAmount) {
                var largest = 0;
                for (int index = 1; index < remainders.length; index++) {
                    if (remainders[index] > remainders[largest]) largest = index;
                }
                amounts[largest]++;
                remainders[largest] = -1;
                assigned++;
            }
            for (int index = 0; index < amounts.length; index++) {
                if (amounts[index] > 0) result.get(index).add(
                        new SpaceObjects.AsteroidMaterial(material.block(), amounts[index]));
            }
        }
        return result.stream().map(List::copyOf).toList();
    }

    public enum ArrivalOutcome {

        SAFE_APPROACH,
        ROCKET_DESTRUCTION,
        ASTEROID_FRAGMENTATION,
        HEAVY_IMPACT
    }

    public enum FragmentationMode {

        NONE,
        CHIPPING,
        CATASTROPHIC
    }

    public record AsteroidFragment(float mass, float radius,
                                   List<SpaceObjects.AsteroidMaterial> materials) {

        public AsteroidFragment {

            materials = List.copyOf(materials);
        }
    }

    public record ImpactPrediction(ArrivalOutcome outcome, double relativeSpeedMetersPerSecond,
                                   double kineticEnergyJoules, int craterRadiusBlocks,
                                   FragmentationMode fragmentationMode, List<AsteroidFragment> fragments,
                                   float remainingTargetMass,
                                   List<SpaceObjects.AsteroidMaterial> recoverableMaterials,
                                   int landingX, int landingZ, double explosiveEnergyJoules) {

        public ImpactPrediction(ArrivalOutcome outcome, double speed, double kinetic, int crater,
                                FragmentationMode mode, List<AsteroidFragment> fragments, float remaining,
                                List<SpaceObjects.AsteroidMaterial> materials, int x, int z) {

            this(outcome, speed, kinetic, crater, mode, fragments, remaining, materials, x, z, 0);
        }

        public double totalEnergyJoules() {

            return kineticEnergyJoules + explosiveEnergyJoules;
        }

        public int fragmentCount() {

            return fragments.size();
        }
    }
}
