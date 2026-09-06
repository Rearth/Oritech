package rearth.oritech.spaceage.simulation;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Shared, deliberately small balancing model for planner collision and landing predictions. */
public final class AsteroidImpactRules {

    public static final double KILOGRAMS_PER_ASTEROID_MASS = 1_000;
    public static final double SPACE_DRAG_PER_SECOND = 0.000002;
    public static final int LANDING_UNCERTAINTY_BLOCKS = 64;
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
        double asteroidMass = attached == null ? 0 : attached.mass() * KILOGRAMS_PER_ASTEROID_MASS;
        double combinedMass = Math.max(1, rocketMass + asteroidMass);
        double energy = combinedMass * relativeSpeed * relativeSpeed * 0.5;
        int landingX = action.landingX() + action.landingOffsetX();
        int landingZ = action.landingZ() + action.landingOffsetZ();

        if (target.type() == SpaceObjects.ObjectType.ASTEROID) {
            double targetMass = Math.max(1, target.mass() * KILOGRAMS_PER_ASTEROID_MASS);
            double energyPerKilogram = energy / targetMass;
            if (energyPerKilogram >= ASTEROID_CHIP_ENERGY_PER_KILOGRAM) {
                var mode = energyPerKilogram >= ASTEROID_BREAKUP_ENERGY_PER_KILOGRAM
                        ? FragmentationMode.CATASTROPHIC : FragmentationMode.CHIPPING;
                var fragments = createFragments(target, energy, energyPerKilogram, mode);
                double fragmentedMass = fragments.stream().mapToDouble(AsteroidFragment::mass).sum();
                float remainingMass = mode == FragmentationMode.CATASTROPHIC ? 0
                        : (float) Math.max(0, target.mass() - fragmentedMass);
                return new ImpactPrediction(ArrivalOutcome.ASTEROID_FRAGMENTATION, relativeSpeed, energy, 0,
                        mode, fragments, remainingMass, List.of(), landingX, landingZ);
            }
            return new ImpactPrediction(canConnectAsteroid(relativeSpeed)
                    ? ArrivalOutcome.SAFE_APPROACH : ArrivalOutcome.ROCKET_DESTRUCTION,
                    relativeSpeed, energy, 0, FragmentationMode.NONE, List.of(), target.mass(),
                    List.of(), landingX, landingZ);
        }

        double safeSpeed = attached == null ? MAX_ASTEROID_CONNECTION_SPEED : SAFE_ASTEROID_SPEED;
        ArrivalOutcome outcome = relativeSpeed <= safeSpeed ? ArrivalOutcome.SAFE_APPROACH
                : energy >= HEAVY_IMPACT_ENERGY || attached != null
                ? ArrivalOutcome.HEAVY_IMPACT : ArrivalOutcome.ROCKET_DESTRUCTION;
        int craterRadius = outcome == ArrivalOutcome.HEAVY_IMPACT
                ? Mth.clamp((int) Math.round(3 * Math.cbrt(energy / HEAVY_IMPACT_ENERGY)), 3, 48) : 0;
        double recovery = relativeSpeed <= safeSpeed ? 1 : Mth.clamp(0.85 - relativeSpeed / 500, 0.15, 0.8);
        var recovered = attached == null ? List.<SpaceObjects.AsteroidMaterial>of()
                : scaleMaterials(attached.materials(), recovery);
        return new ImpactPrediction(outcome, relativeSpeed, energy, craterRadius, FragmentationMode.NONE,
                List.of(), 0, recovered, landingX, landingZ);
    }

    public static List<SpaceObjects.AsteroidMaterial> scaleMaterials(
            List<SpaceObjects.AsteroidMaterial> materials, double fraction) {
        return materials.stream().map(material -> new SpaceObjects.AsteroidMaterial(
                        material.block(), Math.max(0, (int) Math.floor(material.amount() * fraction))))
                .filter(material -> material.amount() > 0).toList();
    }

    private static List<AsteroidFragment> createFragments(SpaceSimulation.SpaceObjectData target, double energy,
                                                          double energyPerKilogram, FragmentationMode mode) {
        int count = mode == FragmentationMode.CHIPPING
                ? Mth.clamp(2 + (int) (energyPerKilogram / 75_000), 2, 4)
                : Mth.clamp(4 + (int) (energyPerKilogram / 250_000), 4, 8);
        double fragmentedFraction = mode == FragmentationMode.CATASTROPHIC ? 1
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
        for (int index = 0; index < count; index++) fractions[index] = fragmentedFraction * weights[index] / totalWeight;

        var materials = distributeMaterials(target.materials(), fractions);
        var fragments = new ArrayList<AsteroidFragment>();
        for (int index = 0; index < count; index++) {
            float mass = (float) (target.mass() * fractions[index]);
            float radius = (float) (target.radius() * Math.cbrt(fractions[index]));
            fragments.add(new AsteroidFragment(mass, radius, materials.get(index)));
        }
        fragments.sort(Comparator.comparingDouble(AsteroidFragment::mass).reversed());
        return List.copyOf(fragments);
    }

    private static List<List<SpaceObjects.AsteroidMaterial>> distributeMaterials(
            List<SpaceObjects.AsteroidMaterial> source, double[] fractions) {
        var result = new ArrayList<List<SpaceObjects.AsteroidMaterial>>();
        for (int index = 0; index < fractions.length; index++) result.add(new ArrayList<>());
        double fragmentedFraction = java.util.Arrays.stream(fractions).sum();
        for (var material : source) {
            int fragmentedAmount = (int) Math.round(material.amount() * fragmentedFraction);
            var amounts = new int[fractions.length];
            var remainders = new double[fractions.length];
            int assigned = 0;
            for (int index = 0; index < fractions.length; index++) {
                double exact = fragmentedAmount * fractions[index] / fragmentedFraction;
                amounts[index] = (int) Math.floor(exact);
                remainders[index] = exact - amounts[index];
                assigned += amounts[index];
            }
            while (assigned < fragmentedAmount) {
                int largest = 0;
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
                                   int landingX, int landingZ) {
        public int fragmentCount() {
            return fragments.size();
        }
    }
}
